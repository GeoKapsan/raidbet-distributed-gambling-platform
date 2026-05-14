package worker;

import game.Game;
import shared.GameSearch;
import shared.Request;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.security.MessageDigest;


public class WorkerHandler implements Runnable {

    private final Socket clientSocket;
    private final Worker worker;

    private static final float[] LOW    = {0f, 0f, 0f, 0.1f, 0.5f, 1f, 1.1f, 1.3f, 2f, 2.5f};
    private static final float[] MEDIUM = {0f, 0f, 0f, 0f, 0f, 0.5f, 1f, 1.5f, 2.5f, 3.5f};
    private static final float[] HIGH   = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 2f, 6.5f};

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
            System.out.println("[Worker:" + worker.getPort() +"] Received " + request.getType() + " request from " + clientSocket.getInetAddress());

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
            case MODIFY_GAME: return handleModifyGame(request);
            case RATE_GAME: return handleRateGame(request);
            case SEARCH, PLAYER_PROFIT, PROVIDER_PROFIT: return handleMapTask(request);
            case PLAY: return handlePlay(request);

            default:
                Request response = new Request(Request.Type.RESPONSE);
                response.put("status", "ERROR");

                return response;
        }
    }

    private Request handleAddGame(Request request) {
        Game game = (Game) request.get("game");
        byte[] image = (byte[]) request.get("image");
        Request response = new Request(Request.Type.RESPONSE);

        boolean addedGame = worker.addGame(game, image);
        if (!addedGame) {
            response.put("status", "ERROR");
            response.put("message", "Game" + game.getGameName() + " already exists.");
        } else {
            response.put("message", "Game" + game.getGameName() + " added successfully.");
            response.put("status", "OK");
        }

        return response;
    }

    private Request handleRemoveGame(Request request) {
        String gameName = (String) request.get("gameName");

        Request response = new Request(Request.Type.RESPONSE);

        boolean removedGame = worker.removeGame(gameName);
        if (removedGame) {
            response.put("status", "OK");
            response.put("message", "Game '" + gameName + "' removed successfully.");
        } else {
            response.put("status", "ERROR");
            response.put("message", "Game '" + gameName + "' not found.");
        }

        return response;
    }

    private Request handleModifyGame(Request request) {
        String gameName = (String) request.get("gameName");
        Request response = new Request(Request.Type.RESPONSE);

        Game game = worker.getGame(gameName);

        // Game does not exist in Worker's game list
        if (game == null) {
            response.put("status", "ERROR");
            response.put("message", "Game '" + gameName + "' not found.");
            return response;
        }

        // Apply only the fields that were sent
        boolean changed = false;

        if (request.containsKey("riskLevel")) {
            String newRisk = (String) request.get("riskLevel");

            game.setRiskLevel(newRisk);

            System.out.println("[Worker:" + worker.getPort() + "] " + gameName + " riskLevel → " + newRisk);

            // Raise flag
            changed = true;
        }

        if (request.containsKey("minBet")) {
            float newMin = (float) request.get("minBet");

            // Guard: minBet must be less than current (or new) maxBet
            float effectiveMax = request.containsKey("maxBet") ? (float) request.get("maxBet") : game.getMaxBet();

            if (newMin >= effectiveMax) {
                response.put("status", "ERROR");
                response.put("message", "minBet must be less than maxBet.");
                return response;
            }

            game.setMinBet(newMin);

            System.out.println("[Worker:" + worker.getPort() + "] " + gameName + " minBet → " + newMin);

            // Raise flag
            changed = true;
        }

        if (request.containsKey("maxBet")) {
            float newMax = (float) request.get("maxBet");

            // Guard: maxBet must be greater than current minBet (minBet may already have been updated above)
            if (newMax <= game.getMinBet()) {
                response.put("status", "ERROR");
                response.put("message", "maxBet must be greater than minBet.");
                return response;
            }

            game.setMaxBet(newMax);

            System.out.println("[Worker:" + worker.getPort() + "] " + gameName + " maxBet → " + newMax);

            // Raise flag
            changed = true;
        }

        // No changes given from player
        if (!changed) {
            response.put("status", "ERROR");
            response.put("message", "No valid fields provided to update.");
            return response;
        }

        response.put("status", "OK");
        response.put("message", "Game '" + gameName + "' updated successfully."
                + " Risk=" + game.getRiskLevel()
                + " MinBet=" + game.getMinBet()
                + " MaxBet=" + game.getMaxBet()
                + " Category=" + game.getBettingCategory());
        return response;
    }

    private Request handleRateGame(Request request) {

        Request response = new Request(Request.Type.RESPONSE);

        String gameName = (String) request.get("gameName");
        Game game = worker.getGame(gameName);

        // Game does not exist/is not active
        if (game == null || !(game.isActive())) {
            response.put("status", "ERROR");
            response.put("message", "Game '" + gameName + "' doesn't exist");
            return response;
        }

        int stars = (int) request.get("stars");
        game.rate(stars);

        response.put("status", "OK");
        return response;
    }

    private Request handleMapTask(Request request) {

        // Saves results from map
        ArrayList<Object> results = getResults(worker.getAllGames(), request);

        // build Request for reducer according
        int mapId = (int) request.get("mapId");

        Request requestToReducer = new Request(request.getType());
        requestToReducer.put("mapId", mapId);
        requestToReducer.put("map_result", results);

        // Send map result to reducer, must be done before we send response back to Master
        sendToReducer(requestToReducer);

        Request response = new Request(Request.Type.RESPONSE);
        response.put("status", "OK");
        return response;
    }

    /**
     * Map function. Filters the Worker's game list to find the games that satisfy the filters the players give.
     * @param games all the Worker's games
     * @param request SEARCH/PROVIDER_PROFIT/PLAYER_PROFIT Request
     * @return results
     */
    private ArrayList<Object> getResults(ArrayList<Game> games, Request request) {
        ArrayList<Object> result = new ArrayList<>();

        switch (request.getType()) {
            case SEARCH:
                for (Game game : games) {
                    if (game.satisfiesFilters(request)) {

                        String safeName = game.getGameName().replaceAll("[^a-zA-Z0-9]", "_");
                        String logoPath = "images/" + safeName + ".png";

                        byte[] logoBytes = null;
                        try {
                            logoBytes = Files.readAllBytes(Paths.get(logoPath));
                        } catch (Exception e) {
                            System.err.println("Fail to read logo file: " + logoPath);
                        }

                        GameSearch gameSearch = new GameSearch(game.getGameName(), logoBytes, game.getMinBet(), game.getMaxBet(), game.getRiskLevel(), game.getBettingCategory(), game.getStars(), game.getJackpot());

                        result.add(gameSearch);

                    }
                }

                break;

            case PLAYER_PROFIT:
                float resultProfit = worker.getPlayerProfit((String) request.get("playerName"));
                if (!Float.isNaN(resultProfit))
                    result.add(String.valueOf(resultProfit));

                break;

            case PROVIDER_PROFIT:
               float profit;
                for (Game game : games) {
                    profit = worker.getGameProfit(game.getGameName());
                    if (game.getProviderName().equals(request.get("providerName")) && !Float.isNaN(profit))
                        result.add(game.getGameName() + ":" + profit);
                }

                break;
        }

        return result;
    }

    private void sendToReducer(Request request) {

        // Connect to Reducer
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
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Request handlePlay(Request request) {

        Request response = new Request(Request.Type.RESPONSE);

        String gameName = (String) request.get("gameName");
        Game playedGame = worker.getGame(gameName);

        if (playedGame == null || !playedGame.isActive()) {
            response.put("status", "ERROR");
            response.put("message", "Game '" + gameName + "' doesn't exist");
            return response;
        }

        float bettingAmount = (float) request.get("bettingAmount");

        // Check betting amount validity
        if (bettingAmount > playedGame.getMaxBet()){
            response.put("status", "ERROR");
            response.put("message", "Betting amount too large");
            return response;
        } else if (bettingAmount < playedGame.getMinBet()) {
            response.put("status", "ERROR");
            response.put("message", "Betting amount too low");
            return response;
        }

        // Request is valid
        Request srgRequest = new Request(Request.Type.GIVE_NUMBER);

        srgRequest.put("gameName", gameName);

        // Request random number from SRG
        Request srgResponse = sendToSrg(srgRequest);
        if  (srgResponse == null) {
            response.put("status", "ERROR from Srg");
            return response;}

        int number = (int) srgResponse.get("number");
        String hashedNumber = (String) srgResponse.get("hashedNumber");

        if (hashedNumber.equals(sha256(number + (String) playedGame.getHashKey()))) {

            float amountWon;

            if (number % 100 == 0) {

                response.put("winStatus", "JACKPOT");
                amountWon = bettingAmount * playedGame.getJackpot();

            } else {
                float[] A = new float[10];

                switch (playedGame.getRiskLevel()) {
                    case "low":

                        A = LOW;
                        break;

                    case "medium":

                        A = MEDIUM;
                        break;

                    case "high":

                        A = HIGH;
                        break;

                    default:
                        break;
                }

                amountWon = bettingAmount * A[number % 10];

                if (amountWon == 0f) {
                    response.put("winStatus", "LOSS");
                } else {
                    response.put("winStatus", "WIN");
                }
            }

            response.put("amountWon", amountWon);
            response.put("status", "OK");

            String playerId = (String) request.get("playerId");

            worker.updatePlayerProfit(playerId, amountWon - bettingAmount);
            worker.updateGameProfit(playedGame.getGameName(), bettingAmount - amountWon);

        } else {
            response.put("status", "ERROR");
            response.put("message", "Game not added correctly (wrong hash)");
        }

        return response;
    }

    private Request sendToSrg(Request request) {

        try (
                Socket socket = new Socket(worker.getSrgHost(), worker.getSrgPort());

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

    /**
     * Function that builds the hash digest.
     * @param input
     * @return
     */
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
