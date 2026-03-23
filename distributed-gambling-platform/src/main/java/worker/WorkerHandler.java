package worker;

import game.Game;
import shared.Request;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class WorkerHandler implements Runnable {

    private Socket clientSocket;
    private Worker worker;

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
        /*
        Handles...
         */
        switch (request.getType()) {
            case ADD_GAME, REMOVE_GAME:
                Game game = (Game) request.get("game");
                Request response = new Request(Request.Type.RESPONSE);

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

            default:
                return new Request(Request.Type.RESPONSE);
                
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

}
