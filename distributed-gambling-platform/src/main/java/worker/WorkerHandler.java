package worker;

import game.Game;
import shared.Request;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;

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

    public void run() {
        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
                ) {
            output.flush();

            Request request = (Request) input.readObject();

            Request response = handleRequest(request);

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Request handleRequest(Request request) {
        
        Request response = new Request(Request.Type.RESPONSE);


        switch (request.getType()) {
            case ADD_GAME, REMOVE_GAME:
                Game game = (Game) request.get("game");
                switch (request.getType()) {
                    case ADD_GAME:
                        worker.addGame(game);
                        response.put("message", "Game" + game.getGameName() + " added successfully.");
                        break;
                    case REMOVE_GAME:
                        worker.removeGame(game.getGameName());
                        response.put("message", "Game" + game.getGameName() + " removed successfully.");
                        break;
                }

                response.put("status", "OK");
                return response;
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

    private Request handleMapTask(Request request) {
        /*
        Handles...
         */

        // Player filters
        int stars = (Integer) request.get("stars");
        int noOfVotes = (Integer) request.get("noOfVotes");
        double minBet = (Double) request.get("minBet");
        double maxBet = (Double) request.get("maxBet");
        String riskLevel = (String) request.get("riskLevel");

        ArrayList<Game> results = new ArrayList<>(); // saves results from map here

        for (Game game : worker.getAllGames()) {
            if (!game.isActive()) continue;
            if (game.getStars() != stars) continue;
            if (noOfVotes != game.getNoOfVotes()) continue;
            if (minBet > maxBet) continue;
            if (minBet > game.getMinBet()) continue;
            if (maxBet < game.getMaxBet()) continue;
            if (riskLevel == null && !riskLevel.equals(game.getRiskLevel())) continue;

            results.add(game); // game satisfies player's filters so add
        }

        System.out.println("[Worker: " + worker.getPort() + "] map() emitted " + results.size() + " games");

        Request response = new Request(Request.Type.RESPONSE);
        response.put("status", "OK");
        response.put("games",  results);
        return response;
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
