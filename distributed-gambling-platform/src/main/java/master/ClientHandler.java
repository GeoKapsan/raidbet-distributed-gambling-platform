package master;

import game.Game;
import shared.Request;
import java.util.*;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket  clientSocket;
    private final Master master;

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

            Request response = route(request); // route request and send back response to client

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Master] Client handler error: " + e.getMessage());
        }
    }

    private Request route(Request request) {
        switch (request.getType()) {
            case ADD_GAME, REMOVE_GAME, CHANGE_RISK:
                Game game = (Game) request.get("game");
                String gameName = game.getGameName();
                forwardToSrg(game, request.getType());
                return forwardToWorkerAndGetResult(request, master.getWorkerAddress(gameName));
            case SEARCH:
                Request response = handleSearch(request);

                // prosorina xwris toys worker gia na mhn peirajw kwdika stoys workers bazw to na moy gyrnaei OK edw na dw an paei kala to managerconsole gamw ton rapth
                //Request response = new Request(Request.Type.RESPONSE);
                //response.put("status", "OK");
                //response.put("message", "Received by Master (no workers yet)");
                //return response;

            default:
                return new Request(Request.Type.RESPONSE);
        }
    }

    private Request handleSearch(Request request) {

        ArrayList<String> workers = master.getWorkerAddresses();
        int noOfWorkers = workers.size();

        Request[] mapResults = new Request[noOfWorkers];
        Thread[] threads = new Thread[noOfWorkers];

        for (int i = 0; i < noOfWorkers; i++) {
            String workerAddress = workers.get(i);
            final int idx = i;

            threads[i] = new Thread(() -> {
                System.out.println("[Master] MAP_TASK to worker[" + idx + "] " + workerAddress);
                mapResults[idx] = forwardToWorkerAndGetResult(request, workerAddress);
                System.out.println("[Master] MAP_TASK from worker[" + idx + "] " + workerAddress);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Master] Thread interrupted while waiting for map results");
            }
        }

        System.out.println("[Master] MAP_TASK completed");

        // still things to do...
    }

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

    void forwardToSrg(Game game, Type type){

        try (
                Socket worker = new Socket(master.getSrgHost(), master.getSrgPort());

                ObjectOutputStream output = new ObjectOutputStream(worker.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(worker.getInputStream());

            ) {
            
            
            output.flush();
            Request request=new Request(type);
            request.put("gameName", game.getGameName());
            request.put("hashKey", game.getHashKey());
            output.writeObject(request);
            output.flush();
        
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

}
