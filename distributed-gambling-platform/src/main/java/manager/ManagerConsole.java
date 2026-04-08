package manager;

import game.Game;
import shared.Request;

import java.io.*;
import java.net.*;
import java.util.*;

import org.json.simple.JSONObject ;
import org.json.simple.parser.JSONParser;


public class ManagerConsole {

    // save master host and port
    private final String masterHost ;
    private final int masterPort;

    private final Scanner scanner = new Scanner(System.in);

    public ManagerConsole(String masterHost, int masterPort){
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public void start() {

        boolean running = true;

        while (running) {
            printMenu();

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": addGame(); break;
                case "2": removeGame(); break;
                case "3": changeGame(); break;
                case "4": listGames(); break;
                case "0": {
                    System.out.println("Exiting Manager Console.");
                    return;
                }
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- Manager Console ---");
        System.out.println("1. Add Game");
        System.out.println("2. Remove Game");
        System.out.println("3. Change Game ");
        System.out.println("4. List Games");
        System.out.println("0. Exit");
        System.out.print("Select an option: ");
    }


    // Manager operations ----------------------------------------------------------------------------------------------------

    private void addGame() {

        String projectDir = System.getProperty("user.dir");
        String folderPath = projectDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "games" + File.separator;
        System.out.print("Enter the name of the JSON file to add (e.g. game.json): ");
        String fileName = scanner.nextLine().trim();

        if (fileName.isEmpty()) {
            System.out.println("File name cannot be empty.");
            return;
        }

        // Αυτόματη προσθήκη του .json σε περίπτωση που ο χρήστης γράψει μόνο "game"
        if (!fileName.toLowerCase().endsWith(".json")) {
            fileName += ".json";
        }

        File file = new File(folderPath + fileName);

        if (!file.exists() || !file.isFile()) {
            System.out.println("JSON file not found at: " + file.getAbsolutePath());
            return;
        }

        JSONParser parser = new JSONParser();

        try (FileReader reader = new FileReader(file)) {
            // Parse the file directly into a JSONObject
            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            // Extract String values
            String gameName = (String) jsonObject.get("GameName");
            String providerName = (String) jsonObject.get("ProviderName");
            double stars = ((Number) jsonObject.get("Stars")).doubleValue();
            int noOfVotes = ((Number) jsonObject.get("NoOfVotes")).intValue();
            String logoPath = (String) jsonObject.get("GameLogo");
            double minBet = ((Number) jsonObject.get("MinBet")).doubleValue();
            double maxBet = ((Number) jsonObject.get("MaxBet")).doubleValue();
            String riskLevel = (String) jsonObject.get("RiskLevel");
            String hashKey = (String) jsonObject.get("HashKey");

            // Instantiate your Game object
            Game parsedGame = new Game(
                    gameName,
                    providerName,
                    stars,
                    noOfVotes,
                    logoPath,
                    minBet,
                    maxBet,
                    riskLevel,
                    hashKey
            );

            // Request build
            Request request = new Request(Request.Type.ADD_GAME);
            request.put("game", parsedGame);

            // Send to Master
            System.out.println("Sending ADD_GAME request for : " + gameName);
            Request response = sendToMaster(request);

            if (response != null) {
                String status = (String) response.get("status");
                String message = (String) response.get("message");

                if ("OK".equals(status)) {
                    System.out.println("[SUCCESS] Game added: " + gameName + (message != null ? " - " + message : ""));

                    // Εμφάνιση των στοιχείων μόνο εφόσον η προσθήκη ήταν επιτυχής
                    System.out.println("Game parsed successfully:");
                    System.out.println("  Name     : " + parsedGame.getGameName());
                    System.out.println("  Provider : " + parsedGame.getProviderName());
                    System.out.println("  Risk     : " + parsedGame.getRiskLevel());
                    System.out.println("  Bet Cat  : " + parsedGame.getBettingCategory());
                    System.out.println("  Min/Max  : " + parsedGame.getMinBet() + " / " + parsedGame.getMaxBet());
                    System.out.println("-----------------------------------");
                } else {
                    System.out.println("[FAIL] Could not add game: " + gameName + (message != null ? " - " + message : ""));
                }
            } else {
                System.out.println("[FAIL] No response received from Master.");
            }

        } catch (Exception e) {
            System.out.println("Failed to parse file " + file.getName() + ": " + e.getMessage());
        }
    }

    private void removeGame() {
        System.out.print("Enter the name of the game to remove: ");
        String gameName = scanner.nextLine().trim();

        if (gameName.isEmpty()) {
            System.out.println("Game name cannot be empty.");
            return;
        }

        // Build REMOVE_GAME Request
        Request request = new Request(Request.Type.REMOVE_GAME);
        request.put("gameName", gameName);

        // Send Request to Master
        System.out.println("Sending REMOVE_GAME request for: " + gameName);
        Request response = sendToMaster(request);

        // Check for response
        if (response == null) {
            System.out.println("[FAIL] No response from Master.");
            return;
        }

        String status  = (String) response.get("status");
        String message = (String) response.get("message");

        if ("OK".equals(status)) {
            System.out.println("[SUCCESS] Game removed: " + gameName
                    + (message != null ? " - " + message : ""));
        } else {
            System.out.println("[FAIL] Could not remove game: " + gameName
                    + (message != null ? " - " + message : ""));
        }
    }

    /**
     * Allows the manager to change any combination of:
     *   - Risk level  (Low / Medium / High)
     *   - Min bet
     *   - Max bet
     * Fields left blank are not modified.
     */
    private void changeGame() {
        System.out.print("Enter the name of the game to update: ");
        String gameName = scanner.nextLine().trim();

        if (gameName.isEmpty()) {
            System.out.println("Game name cannot be empty.");
            return;
        }

        // --- Risk level (optional) ---
        System.out.print("New risk level (low / medium / high, or ENTER to skip): ");
        String newRiskLevel = scanner.nextLine().trim();

        // --- Min bet (optional) ---
        System.out.print("New minimum bet (or ENTER to skip): ");
        String minBetStr = scanner.nextLine().trim();

        // --- Max bet (optional) ---
        System.out.print("New maximum bet (or ENTER to skip): ");
        String maxBetStr = scanner.nextLine().trim();

        // At least one field must be changed
        if (newRiskLevel.isEmpty() && minBetStr.isEmpty() && maxBetStr.isEmpty()) {
            System.out.println("No changes specified. Operation cancelled.");
            return;
        }

        // Validate numeric inputs
        Double newMinBet = null;
        Double newMaxBet = null;

        if (!minBetStr.isEmpty()) {
            try {
                newMinBet = Double.parseDouble(minBetStr);
                if (newMinBet <= 0) {
                    System.out.println("[FAIL] Min bet must be positive.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("[FAIL] Invalid min bet value.");
                return;
            }
        }

        if (!maxBetStr.isEmpty()) {
            try {
                newMaxBet = Double.parseDouble(maxBetStr);
                if (newMaxBet <= 0) {
                    System.out.println("[FAIL] Max bet must be positive.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("[FAIL] Invalid max bet value.");
                return;
            }
        }

        // Cross-validate min/max if both provided
        if (newMinBet != null && newMaxBet != null && newMinBet >= newMaxBet) {
            System.out.println("[FAIL] Min bet must be less than max bet.");
            return;
        }

        // Build request — only populate fields that were actually changed
        Request request = new Request(Request.Type.CHANGE_RISK);
        request.put("gameName", gameName);

        if (!newRiskLevel.isEmpty()) request.put("riskLevel",  newRiskLevel);
        if (newMinBet    != null)    request.put("minBet",     newMinBet);
        if (newMaxBet    != null)    request.put("maxBet",     newMaxBet);

        // Summary of what we're sending
        System.out.println("Sending CHANGE_RISK request for: " + gameName);
        if (!newRiskLevel.isEmpty()) System.out.println("  Risk level → " + newRiskLevel);
        if (newMinBet    != null)    System.out.println("  Min bet    → " + newMinBet);
        if (newMaxBet    != null)    System.out.println("  Max bet    → " + newMaxBet);

        Request response = sendToMaster(request);

        if (response == null) {
            System.out.println("[FAIL] No response from Master.");
            return;
        }

        String status  = (String) response.get("status");
        String message = (String) response.get("message");

        if ("OK".equalsIgnoreCase(status)) {
            System.out.println("[SUCCESS] Game updated: " + gameName + (message != null ? " - " + message : ""));
        } else {
            System.out.println("[FAIL] Could not update game: " + gameName + (message != null ? " - " + message : ""));
        }
    }

    private void listGames() {
        System.out.println("Fetching the games from  the network...");

        Request request = new Request(Request.Type.SHOW_GAMES);
        Request response = sendToMaster(request);

        if (response == null) {
            System.out.println("[FAIL] No response from Master");
            return ;
        }

        String status = (String) response.get("status");
        if ("OK".equals(status)) {
            ArrayList<Game> games = (ArrayList<Game>) response.get("games");

            if (games == null || games.isEmpty()){
                System.out.println("No games loaded ");
                return ;

            }

            System.out.println("\n--- Available Games (" + games.size() + ") ---");
            for (int i = 0; i < games.size(); i++) {
                Game g = games.get(i);
                System.out.println((i + 1) + ". " + g.getGameName()
                        + " | Provider: " + g.getProviderName()
                        + " | Risk: "     + g.getRiskLevel()
                        + " | Bet: "      + g.getMinBet() + "-" + g.getMaxBet()
                        + " | Category: " + g.getBettingCategory()
                        + " | Active: "   + g.isActive());
            }
            System.out.println("-----------------------------------");
        } else {
            String message = (String) response.get("message");
            System.out.println("[FAIL] Could not fetch games: " + (message != null ? message : "Unknown error"));
        }

    }


    // TCP Helper operation ----------------------------------------------------------------------------------------------------

    // Send Request to Master and return Response from Master
    private Request sendToMaster(Request request) {

        try (
                Socket socket = new Socket(masterHost, masterPort);
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


    public static void main(String[] args) {

        ManagerConsole console = new ManagerConsole("localhost", 5001);
        console.start();
    }
}


