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

        if (shouldReduce) {
            ArrayList<String> results = initiateReduce(mapId, request.getType());

            Request requestToMaster = new Request(Request.Type.REDUCER_CALLBACK);
            requestToMaster.put("mapId", mapId);
            requestToMaster.put("result", results);

            forwardToMaster(requestToMaster);
        }

        Request response = new Request(Request.Type.RESPONSE);
        response.put("status", "OK");
        return response;
    }


    private ArrayList<String> initiateReduce(int mapId, Request.Type type) {
        ArrayList<String> results = reducer.getCollectedResults(mapId);

        reducer.cleanup(mapId);

        switch (type) {
            case SEARCH:

                return results;

            case PROVIDER_PROFIT:

                String[] parts;
                double providerProfit = 0.0;

                for (String profitPerGame : results) {
                    parts = profitPerGame.split(":");
                    providerProfit += Double.parseDouble(parts[1]);
                }

                String providerProfitStr = Double.toString(providerProfit);

                if (providerProfit > 0.0)
                    providerProfitStr = "+"  + providerProfitStr;

                results.add(providerProfitStr);

                break;

            case PLAYER_PROFIT:

                double profit = 0.0;

                for (String profitPerGame : results) {
                    profit += Double.parseDouble(profitPerGame);
                }

                // Remove all elements
                results.clear();

                String profitStr = Double.toString(profit);

                if (profit > 0.0)
                    profitStr = "+" + profitStr;

                results.add(profitStr);

                break;

            default:
                break;
        }

        return results;
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