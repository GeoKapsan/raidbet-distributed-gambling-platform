package worker;

import game.Game;
import shared.Request;

import java.io.*;
import java.net.*;
import java.util.*;


public class WorkerHandler implements Runnable {

    private Socket clientSocket;
    private Worker worker;

    public WorkerHandler(Socket clientSocket, Worker worker) {
        this.clientSocket = clientSocket;
        this.worker = worker;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
                ) {
            output.flush();

            Request request = (Request) input.readObject();
            System.out.println("[Worker] Received " + request.getType() + " request from " + clientSocket.getInetAddress());

            Request response = handle(request);

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Request handle(Request request) {
        switch (request.getType()) {
            case ADD_GAME: return handleAddGame(request);
            case REMOVE_GAME: return handleRemoveGame(request);
            case CHANGE_RISK: return handleChangeRisk(request);
            case SEARCH: return handleMapTask(request);
            default:
                return new Request(Request.Type.RESPONSE);
                
        }
    }

    private Request handleAddGame(Request request) {
        Game game = (Game) request.get("game");
        Request response = new Request(Request.Type.RESPONSE);
        worker.addGame(game);
        response.put("message", "Game" + game.getGameName() + " added successfully.");
        response.put("status", "OK");
        return response;
    }

    private Request handleRemoveGame(Request request) {
        String gameName = (String) request.get("gameName");
        Request response = new Request(Request.Type.RESPONSE);
        worker.removeGame(gameName);
        response.put("message", "Game" + gameName + " removed successfully.");
        response.put("status", "OK");
        return response;
    }

    private Request handleChangeRisk(Request request) {

    }

    private Request handleMapTask(Request request) {

        // saves results from map
        ArrayList<String[]> results = mapFilters((int) request.get("mapId"), worker.getAllGames(), request);

        System.out.println("[Worker: " + worker.getPort() + "] map() emitted " + results.size() + " games");

        // send map result to reducer, must be done before we send response back to Master
        sendToReducer((int) request.get("mapId"), (int) request.get("noOfWorkers"), results);

        Request response = new Request(Request.Type.RESPONSE);
        response.put("status", "OK");
        return response;
    }

    private ArrayList<String[]> mapFilters(int key, ArrayList<Game> games, Request filters) {
        ArrayList<String[]> result = new ArrayList<>();

        String[] resultTuple = new String[2];
        for (Game game : games) {
            if (game.satisfiesFilters(filters)) {
                resultTuple[0] = Integer.toString(key);
                resultTuple[1] = game.getGameName();
                result.add(resultTuple);
            }
        }
        return result;
    }

    private void sendToReducer(int mapId, int noOfWorkers, ArrayList<String[]> results) {

        // build Request for reducer
        Request request = new Request(Request.Type.SEARCH);
        request.put("mapId", mapId);
        request.put("noOfWorkers", noOfWorkers);
        request.put("map_result", results);

        // connect to Reducer
        String reducerHost = worker.getReducerHostAndPort().split(":")[0];
        int reducerPort = Integer.parseInt(worker.getReducerHostAndPort().split(":")[1]);
        try (
                Socket reducer = new Socket(reducerHost, reducerPort);

                ObjectOutputStream output = new ObjectOutputStream(reducer.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(reducer.getInputStream());
        ) {

            output.flush();

            output.writeObject(request);

            output.flush();

            Request response = (Request) input.readObject();

            // TO-DO handle response by printing something on screen

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
