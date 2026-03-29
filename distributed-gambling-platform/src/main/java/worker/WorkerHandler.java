package worker;

import game.Game;
import shared.Request;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;


public class WorkerHandler implements Runnable {

    private Socket clientSocket;
    private Worker worker;

    private static final double[] LOW    = {0.0, 0.0, 0.0, 0.1, 0.5, 1.0, 1.1, 1.3, 2.0, 2.5};
    private static final double[] MEDIUM = {0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 1.5, 2.5, 3.5};
    private static final double[] HIGH   = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 6.5};

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
            case PLAY:

                Request srgRequest=new Request("GIVE_NUMBER");
                srgRequest.put("gameName", request.get("gameName"));
                Request srgResponse=sendToSrg(srgRequest);

                int number= (Integer) srgResponse.get("number");
                String hashedNumber= (String) srgResponse.get("hashedNumber");
                Game playedGame= (Game)request.get("game");

                if (hashedNumber==sha256(number+(String) playedGame.getHashKey())){

                    double jackpot;
                    double[] A;
                    switch (playedGame.getRiskLevel()) {
                        case "low":

                            jackpot=10.0;
                            A=LOW;
                            break;
                        
                        case "medium":

                            jackpot=20.0;
                            A=MEDIUM;
                            break;

                        case "high":

                            jackpot=40.0;
                            A=HIGH;
                            break;

                        default:
                            break;
                    }

                    double amountWon;
                    double bettingAmount=(Double) request.get("bettingAmount");
                    if (number%100==0){
                        response.put("status", "JACKPOT!!!");
                        amountWon=bettingAmount*jackpot;
                    }else{
                        response.put("status", "NOT JACKPOT");
                        amountWon=bettingAmount*A[number%10];
                    }

                    response.put("amountWon", amountWon);
                }else{
                    response.put("status", "ERROR(wrong hash)");
                }
                
                return response;
            default:
                return response;
                
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
      
    private Request sendToSrg(Request request) {

        try (
                Socket socket = new Socket(srgHost, srgPort);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        ) {

            oos.writeObject(request);
            oos.flush();

            return (Request) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[ERROR] Could not communicate with Master: " + e.getMessage());
            return null;
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
