package reducer;

import shared.Request;

import java.io.*;
import java.net.*;
import java.util.*;


public class ReducerHandler implements Runnable {

    private Socket clientSocket;
    private Reducer reducer;

    public ReducerHandler(Socket clientSocket, Reducer reducer) {
        this.clientSocket = clientSocket;
        this.reducer = reducer;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
        ) {
            output.flush();

            Request request = (Request) input.readObject();

            Request response = handleSearch(request);

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Request handleSearch(Request request) {
        int mapId = (int) request.get("mapId");

        if (!reducer.mapIdRegistered(mapId)) reducer.registerMapReduce(mapId);

        ArrayList<String[]> games = (ArrayList<String[]>) request.get("map_result");

        boolean shouldReduce = reducer.collect(mapId, games);

        if (shouldReduce) initiateReduce(mapId);

        Request response = new Request(Request.Type.RESPONSE);
        response.put("status", "OK");
        return response;
    }

    private void initiateReduce(int mapId) {
        ArrayList<String> games = reducer.reduce(mapId, reducer.getCollectedGames(mapId));

        reducer.cleanup(mapId);

        Request request = new  Request(Request.Type.REDUCER_CALLBACK);
        request.put("mapId", mapId);
        request.put("gameNames", games);

        Request response = forwardToMaster(request);
    }

    private Request forwardToMaster(Request request) {

        String[] parts = reducer.getMasterHostAndPort().split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        System.out.println("[Reducer] Forwarding to master: " + host + ":" + port);

        try (
                Socket master = new Socket(host, port);

                ObjectOutputStream output = new ObjectOutputStream(master.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(master.getInputStream());
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

}