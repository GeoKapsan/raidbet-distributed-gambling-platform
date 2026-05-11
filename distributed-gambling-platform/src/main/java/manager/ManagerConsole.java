package manager;

import game.Game;
import master.Master;
import shared.Request;

import java.io.*;
import java.net.*;
import java.util.*;

import org.json.simple.JSONObject ;
import org.json.simple.parser.JSONParser;


public class ManagerConsole {

    // Save master host and port
    private final String masterHost ;
    private final int masterPort;

    private final Scanner scanner = new Scanner(System.in);

    public ManagerConsole(String masterHost, int masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public void start() {

        while (true) {
            printMenu();

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": addGame(); break;
                case "2": removeGame(); break;
                case "3": modifyGame(); break;
                case "4": listGames(); break;
                case "5": providerProfit(); break;
                case "6": playerProfit(); break;
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
        System.out.println("5. Profit/Loss for a provider");
        System.out.println("6. Profit/Loss for a player");
        System.out.println("0. Exit");
        System.out.print("Select an option: ");
    }


    // Manager operations ----------------------------------------------------------------------------------------------------

    private void addGame() {

        String projectDir = System.getProperty("user.dir");
        String folderPath = projectDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "games" + File.separator;
        System.out.print("Enter the name of the JSON file to add: ");
        String fileName = scanner.nextLine().trim();

        if (fileName.isEmpty()) {
            System.out.println("[FAIL] File name cannot be empty.");
            return;
        }

        if (!fileName.toLowerCase().endsWith(".json")) {
            fileName += ".json";
        }

        // Add file
        File file = new File(folderPath + fileName);

        if (!file.exists() || !file.isFile()) {
            System.out.println("[FAIL] JSON file not found at: " + file.getAbsolutePath());
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
            float minBet = ((Number) jsonObject.get("MinBet")).floatValue();
            float maxBet = ((Number) jsonObject.get("MaxBet")).floatValue();
            String riskLevel = (String) jsonObject.get("RiskLevel");
            String hashKey = (String) jsonObject.get("HashKey");

            // Instantiate Game object
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
                    System.out.println("Game added: " + gameName + (message != null ? " - " + message : ""));

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
            System.out.println("[FAIL] Failed to parse file " + file.getName() + ": " + e.getMessage());
        }
    }

    private void removeGame() {

        // Insert game name to remove
        System.out.print("Enter the name of the game to remove: ");
        String gameName = scanner.nextLine().trim();
        if (gameName.isEmpty()) {
            System.out.println("[FAIL] Game name cannot be empty.");
            return;
        }

        // Build REMOVE_GAME Request
        Request request = new Request(Request.Type.REMOVE_GAME);
        request.put("gameName", gameName);

        // Send Request to Master
        System.out.println("Sending REMOVE_GAME request for: '" + gameName + "'");

        Request response = sendToMaster(request);

        // Check for response
        if (response == null) {
            System.out.println("[FAIL] No response from Master.");
            return;
        }

        String status  = (String) response.get("status");
        String message = (String) response.get("message");

        if (!"OK".equals(status)) {
            System.out.println("[FAIL] " + message);
            return;
        }

        System.out.println("Game removed successfully");
    }

    /**
     * Allows the manager to change any combination of:
     *   - Risk level  (Low / Medium / High)
     *   - Min bet
     *   - Max bet
     * Fields left blank are not modified.
     */
    private void modifyGame() {
        System.out.print("Enter the name of the game to update: ");
        String gameName = scanner.nextLine().trim();

        if (gameName.isEmpty()) {
            System.out.println("[FAIL] Game name cannot be empty.");
            return;
        }

        // --- Risk level (optional) ---
        System.out.print("New risk level (low/medium/high, or ENTER to skip): ");
        String newRiskLevel = scanner.nextLine().trim();
        if (!newRiskLevel.isEmpty()) {
            if (!"low".equals(newRiskLevel) && !"medium".equals(newRiskLevel) && !"high".equals(newRiskLevel)) {
                System.out.println("[FAIL] Invalid risk level.");
                return;
            }
        }

        // --- Min bet (optional) ---
        System.out.print("New minimum bet (or ENTER to skip): ");
        String minBetStr = scanner.nextLine().trim();

        // --- Max bet (optional) ---
        System.out.print("New maximum bet (or ENTER to skip): ");
        String maxBetStr = scanner.nextLine().trim();

        // At least one field must be changed
        if (newRiskLevel.isEmpty() && minBetStr.isEmpty() && maxBetStr.isEmpty()) {
            System.out.println("[FAIL] No changes specified. Operation cancelled.");
            return;
        }

        if (minBetStr.isEmpty() ^ maxBetStr.isEmpty()) {
            System.out.println("[FAIL] Either both or none of the values must be specified.");
        }

        // Validate numeric inputs
        Float newMinBet = null;
        Float newMaxBet = null;

        if (!minBetStr.isEmpty()) {
            try {
                newMinBet = Float.parseFloat(minBetStr);
                if (newMinBet <= 0f) {
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
                newMaxBet = Float.parseFloat(maxBetStr);
                if (newMaxBet <= 0f) {
                    System.out.println("[FAIL] Max bet must be positive.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("[FAIL] Invalid max bet value.");
                return;
            }
        }

        // Build request — only create fields that were actually changed
        Request request = new Request(Request.Type.MODIFY_GAME);
        request.put("gameName", gameName);

        if (!newRiskLevel.isEmpty()) request.put("riskLevel",  newRiskLevel);
        if (newMinBet    != null)    request.put("minBet",     newMinBet);
        if (newMaxBet    != null)    request.put("maxBet",     newMaxBet);

        // Summary
        System.out.println("Sending CHANGE_RISK request for: " + gameName);
        if (!newRiskLevel.isEmpty()) System.out.println("  Risk level: " + newRiskLevel);
        if (newMinBet    != null)    System.out.println("  Min bet: " + newMinBet);
        if (newMaxBet    != null)    System.out.println("  Max bet: " + newMaxBet);


        Request response = sendToMaster(request);

        if (response == null) {
            System.out.println("[FAIL] No response from Master.");
            return;
        }

        String status  = (String) response.get("status");
        String message = (String) response.get("message");

        if (!"OK".equals(status)) {
            System.out.println("[FAIL] " + message);
            return;
        }

        System.out.println("Game updated: " + gameName + (message != null ? " - " + message : ""));
    }

    private void listGames() {
        System.out.println("Fetching the games from  the network...");

        Request request = new Request(Request.Type.SEARCH);
        Request response = sendToMaster(request);

        if (response == null) {
            System.out.println("[FAIL] No response from Master");
            return ;
        }

        ArrayList<String> gameNames = (ArrayList<String>) response.get("result");
        if (gameNames == null || gameNames.isEmpty()) {
            System.out.println("[FAIL] Workers don't have any games.");
            return;
        }

        System.out.println("\n── All Available games ──────────────────────────");
        String gameName;
        for (String g : gameNames) {
            gameName = g.split(":")[0];

            System.out.println("\t" + gameName);
            System.out.println("\t-----------------------------------");
        }
        System.out.println("────────────────────────────────────────");

    }
    private void providerProfit(){
        System.out.print("Enter provider name: ");
        String choice = scanner.nextLine().trim();

        Request request = new Request(Request.Type.PROVIDER_PROFIT);
        request.put("providerName", choice);

        // Send Request to Master
        Request response = sendToMaster(request);

        // Receive response
        ArrayList<String> result = (ArrayList<String>) response.get("result");

        if (result == null) {
            System.out.println("[FAIL] No result from Master.");
            return;
        }

        if (result.isEmpty()) {
            System.out.println("No data for " + "\"" + choice + "\".");
            return;
        }

        for (int i = 0; i < result.size() - 1; i++) {
            System.out.println(result.get(i));
        }

        System.out.println("Total: " + result.getLast() + " FUN");
    }

    private void playerProfit() {
        System.out.print("Enter player name: ");
        String choice = scanner.nextLine().trim();

        Request request = new Request(Request.Type.PLAYER_PROFIT);
        request.put("playerName", choice);

        // Send Request to Master
        Request response = sendToMaster(request);
        ArrayList<String> result = (ArrayList<String>) response.get("result");

        if (result == null || result.isEmpty()) {
            System.out.println("[FAIL] No result from Master.");
            return;
        }

        System.out.println("Total Profit/Loss: " + result.getFirst() + " FUN");
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
        Properties config = new Properties();
        try (
                InputStream in = Master.class.getClassLoader().getResourceAsStream("config/config.properties")
        ) {

            if (in == null) throw new RuntimeException("config/config.properties not found in classpath");

            config.load(in);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String masterHost  = config.getProperty("master.host");
        int masterPort     = Integer.parseInt(config.getProperty("master.port"));

        // Initialize and start Master console app
        ManagerConsole console = new ManagerConsole(masterHost, masterPort);
        console.start();
    }

}


