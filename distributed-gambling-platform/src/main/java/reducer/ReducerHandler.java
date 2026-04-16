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

            Request response = handle(request);

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Request handle(Request request) {
        int mapId = (int) request.get("mapId");

        if (!reducer.mapIdRegistered(mapId)) reducer.registerMapReduce(mapId);

        ArrayList<String> result = (ArrayList<String>) request.get("map_result");

        boolean shouldReduce = reducer.collect(mapId, result);

        if (shouldReduce) initiateReduce(mapId, request.getType());

        Request response = new Request(Request.Type.RESPONSE);
        response.put("status", "OK");
        return response;
    }


    private void initiateReduce(int mapId, Request.Type type) {
        ArrayList<String> results= reducer.getCollectedResults(mapId);

        reducer.cleanup(mapId);

        Request request = new  Request(Request.Type.REDUCER_CALLBACK);
        request.put("mapId", mapId);

        switch (type) {
            case SEARCH:
                request.put("result", results);
                break;

            case PROVIDER_PROFIT:
                if (results.isEmpty())
                    request.put("result", "Invalid provider name or no games have been played under this provider");
                else {
                    String[] parts;
                    double profit=0.0;

                    for (String profitPerGame : results) {
                        parts = profitPerGame.split(":");
                        profit+= Double.parseDouble(parts[1]);
                    }

                    results.add("Total:"+profit+" FUN");
                    request.put("result", results);
                }

                break;

            case PLAYER_PROFIT:
                if (results.isEmpty())
                    request.put("result", "Invalid player name");
                else {
                    double profit=0.0;
                    for (String profitPerGame : results) {
                        profit+=Double.parseDouble(profitPerGame);
                    }
                    request.put("result", "Total Profit/Loss:"+profit+" FUN");
                }
                break;
        }


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