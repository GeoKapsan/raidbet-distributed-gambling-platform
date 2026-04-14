package master;

import game.Game;
import shared.Request;
import java.util.*;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket  clientSocket;
    private final Master master;

    private static final long TIMEOUT_MS = 30_000; // 30 s

    public ClientHandler(Socket clientSocket, Master master) {
        this.clientSocket = clientSocket;
        this.master = master;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
                )
        {
            output.flush(); // send header to avoid deadlock

            Request request = (Request) input.readObject(); // receive client's data
            System.out.println("[Master] Received " + request.getType() + " request from " + clientSocket.getInetAddress());

            Request response = route(request); // route request and send back response to client

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Master] Client handler error: " + e.getMessage());
        }
    }

    private Request route(Request request) {
        switch (request.getType()) {
            case ADD_GAME:
                Game game = (Game) request.get("game");
                forwardToSrg(request);
                return forwardToWorkerAndGetResult(request, master.getWorkerAddress(game.getGameName()));

            case REMOVE_GAME:
                forwardToSrg(request);
                return forwardToWorkerAndGetResult(request, master.getWorkerAddress((String) request.get("gameName")));

            case MODIFY_GAME:
            case RATE_GAME:
            case PLAY:
                return forwardToWorkerAndGetResult(request, master.getWorkerAddress((String) request.get("gameName")));

            case SEARCH:
                return handleSearch(request);

            case REDUCER_CALLBACK:
                return handleReducerCallback(request);

            case PLAYER_PROFIT:

            case PROVIDER_PROFIT:
            default:
                return new Request(Request.Type.RESPONSE);
        }
    }

    // MapReduce SHOW_GAMES & SEARCH ----------------------------------------------------------------------------------------------------

    private Request handleSearch(Request request) {

        // generate new mapId
        int mapId = master.generateMapId();
        System.out.println("[Master] Initiating SEARCH for mapId=" + mapId);

        // register ClientHandler state in the waiting set before starting map function
        SavedMasterState state = new SavedMasterState();
        master.registerMapReduceOperation(mapId, state);

        // creating MAP_TASK to send to Workers
        ArrayList<String> workers = master.getAllWorkerAddresses();
        int noOfWorkers = workers.size();

        Thread[] threads = new Thread[noOfWorkers];

        for (int i = 0; i < noOfWorkers; i++) {
            String workerAddress = workers.get(i);
            final int idx = i;

            threads[i] = new Thread(() -> {
                System.out.println("[Master] Assigned SEARCH to worker[" + idx + "] " + workerAddress);

                // put number of workers and mapId for the specific map task so the Reducer knows the number of workers for the map task
                request.put("mapId", mapId);
                request.put("noOfWorkers", noOfWorkers);

                Request response = forwardToWorkerAndGetResult(request, workerAddress);

                if (response == null || !"OK".equals(response.get("status"))) {
                    System.err.println("[Master] Worker[" + idx + "] for map Id= " + mapId + " did not respond");
                } else {
                    System.out.println("[Master] Worker[" + idx + "] for mapId=" + mapId + " succeeded");
                }
            });
        }

        // initiate threads
        for (Thread thread : threads) thread.start();

        // wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[Master] All workers responded for mapId=" + mapId);

        // Wait for REDUCER_CALLBACK
        // This ClientThread will suspend waiting for another ClientThread to notify this thread after
        // it receives the REDUCER_CALLBACK with the result
        try {
            ArrayList<String> games = state.waitForResult(TIMEOUT_MS);

            // remove this state from Master, no longer needed
            master.removeSavedMasterState(mapId);

            if (games == null) {
                // Timeout — Reducer never replied (setResult was not executed)
                Request response = new Request(Request.Type.RESPONSE);
                response.put("status",  "ERROR");
                response.put("message", "Search timed out (mapId=" + mapId + ")");
                return response;
            }

            System.out.println("[Master] Received Reduce result for mapId=" + mapId);

            Request response = new Request(Request.Type.RESPONSE);
            response.put("status", "OK");
            response.put("gameNames",  games);
            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            master.removeSavedMasterState(mapId);
            Request response = new Request(Request.Type.RESPONSE);
            response.put("status",  "ERROR");
            response.put("message", "Search interrupted");
            return response;
        }

    }

    /**
     * Handles the REDUCER_CALLBACK. This will run in a separate
     * thread (the one spawned when the Reducer's TCP connection arrived).
     * Finds the waiting SavedMasterState by mapId and wakes the sleeping
     * SEARCH ClientHandler via notify().
     */
    private Request handleReducerCallback(Request request) {
        int mapId = (int) request.get("mapId");
        ArrayList<String> games = (ArrayList<String>) request.get("gameNames");

        System.out.println("[Master] Received REDUCER_CALLBACK result for mapId = " + mapId);

        SavedMasterState state = master.getMasterState(mapId);
        if (state != null) {
            // setResult will wake up suspended ClientHandler thread for specific mapId
            state.setResult(games != null ? games : new ArrayList<>());
        } else
            System.out.println("[Master] No state found for mapId = " + mapId);

        // respond back to Reducer
        Request response = new Request(Request.Type.RESPONSE);
        response.put("status", "OK");
        return response;
    }

  
    // TCP operation helpers ----------------------------------------------------------------------------------------------------
  
    private Request forwardToWorkerAndGetResult(Request request, String workerAddress) {
        String[] parts = workerAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        System.out.println("[Master] Forwarding to worker: " + host + ":" + port);

        try (
                Socket worker = new Socket(host, port);

                ObjectOutputStream output = new ObjectOutputStream(worker.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(worker.getInputStream());
                ) {
            output.flush();
            output.writeObject(request);
            output.flush();
            return (Request) input.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }

    private Request forwardToSrg(Request request){

        try (
                Socket worker = new Socket(master.getSrgHost(), master.getSrgPort());

                ObjectOutputStream output = new ObjectOutputStream(worker.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(worker.getInputStream());

            ) {

            output.flush();

            if (request.containsKey("game")) {
                Game game = (Game) request.get("game");
                request.put("gameName", game.getGameName());
                request.put("hashKey", game.getHashKey());
            }
            else {
                request.put("gameName", request.get("gameName"));
            }

            output.writeObject(request);
            output.flush();

            Request response = (Request) input.readObject();
            return response;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }

}
