package player;

import java.io.*;
import java.net.*;
import java.util.*;

import master.Master;
import shared.Request;
import srg.Srg;


public class Player {

    private String playerId;
    private double balance = 0.0;

    private final String masterHost;
    private final int masterPort;

    private final Scanner scanner = new Scanner(System.in);

    public Player (String playerId, String masterHost, int masterPort) {
        this.playerId = playerId;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public void start() {
        while(true) {
            
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": listGames(); break;
                case "2": play(); break;
                case "3": search(); break;
                case "4": addBalance(); break;
                case "5": rateByGameName(); break;
                case "0": {
                    System.out.println("Exiting Player Console.");
                    return;
                }
                default:
                    System.out.println("[FAIL] Invalid option. Please try again.");
                    break;
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- Player Application ---");
        System.out.println("1. Show Games");
        System.out.println("2. Play");
        System.out.println("3. Search Games");
        System.out.println("4. Add balance");
        System.out.println("5. Rate game");
        System.out.println("0. Exit");
        System.out.print("Select an option: ");
    }

    private void listGames() {
        System.out.println("Fetching the games from  the network...");

        Request request = new Request(Request.Type.SEARCH);
        Request response = sendToMaster(request);

        if (response == null) {
            System.out.println("[FAIL] No response from Master");
            return ;
        }

        ArrayList<String> gameNames = (ArrayList<String>) response.get("gameNames");
        if (gameNames == null || gameNames.isEmpty()) {
            System.out.println("[FAIL] Workers don't have any games.");
            return;
        }

        System.out.println("\n── All Available games ──────────────────────────");
        for (String g : gameNames) {
            System.out.println("\t" + g);
            System.out.println("\t-----------------------------------");
        }
        System.out.println("────────────────────────────────────────");

    }

    private void play() {

        // Insert game name to play
        System.out.print("Game name (ENTER to skip): ");
        String gameName = scanner.nextLine().trim();

        if (gameName.isEmpty()) {
            System.out.println("[FAIL] Game name cannot be empty");
            return;
        }

        // Insert betting amount
        System.out.print("Betting amount (ENTER to skip): ");
        String bettingAmountStr = scanner.nextLine().trim();
        if (bettingAmountStr.isEmpty()) {
            System.out.println("[FAIL] Betting amount cannot be empty");
            return;
        }

        Double bettingAmount;
        try {
            bettingAmount = Double.parseDouble(bettingAmountStr);
        } catch (NumberFormatException e) {
            System.out.println("[FAIL] Betting amount cannot String");
            return;
        }

        // Check validity of inserted betting amount
        String validityMessage = checkBettingAmountValidity(bettingAmount);
        if (!"OK".equals(validityMessage)) {
            System.out.println(validityMessage);
            return;
        }

        // Build Master Request
        Request request = new Request(Request.Type.PLAY);
        request.put("gameName", gameName);
        request.put("bettingAmount", bettingAmount);
        request.put("playerId", playerId);


        // Receive response from Master
        Request response = sendToMaster(request);

        String status = (String) response.get("status");

        if (!"OK".equals(status)) {
            System.out.println("[FAIL] " + response.get("message"));
            return;
        }

        double amountWon = (Double) response.get("amountWon");
        System.out.println("Amount Won: "+ amountWon + " FUN");

        // Update balance for current player
        updateBalance(amountWon - bettingAmount);

        printBalance();
    }

    private String checkBettingAmountValidity(double amount) {
        if (amount <= 0.0) return "[FAIL] Cannot insert negative amount";
        if (amount > balance) return "[FAIL] Not enough balance";
        return "OK";
    }

    private void search() {

        // Build Master Request by player input
        Request request = new Request(Request.Type.SEARCH);
        request.put("playerId", playerId);

        System.out.print("Risk level (low/medium/high, or ENTER to skip): ");
        String risk = scanner.nextLine().trim();
        if (!risk.isEmpty()) request.put("riskLevel", risk);

        System.out.print("Betting category ($, $$, $$$, or ENTER to skip): ");
        String cat = scanner.nextLine().trim();
        if (!cat.isEmpty()) request.put("bettingCategory", cat);

        System.out.print("Stars (1-5, or ENTER to skip): ");
        String starsStr = scanner.nextLine().trim();
        try {
            if (!starsStr.isEmpty()) request.put("stars", Integer.parseInt(starsStr));
        } catch (NumberFormatException e) {
            System.out.println("[FAIL] Stars must be int");
            return;
        }

        // Send to Master
        Request response = sendToMaster(request);

        if (!"OK".equals(response.get("status"))) {
            System.out.println("[FAIL] " + response.get("message"));
            return;
        }

        ArrayList<String> gameNames = (ArrayList<String>) response.get("result");
        if (gameNames == null || gameNames.isEmpty()) {
            System.out.println("No games match your filters");
            return;
        }

        System.out.println("\n── Matching games ──────────────────────────");
        for (String g : gameNames) {
            System.out.println("\t" + g);
            System.out.println("\t-----------------------------------");
        }
        System.out.println("────────────────────────────────────────");
    }

    private void addBalance() {

        // Insert betting amount
        System.out.print("Amount to add (ENTER to skip): ");
        System.out.print("Betting amount (ENTER to skip): ");
        String addedAmountStr = scanner.nextLine().trim();
        if (addedAmountStr.isEmpty()) {
            System.out.println("[FAIL] Game name cannot be empty");
            return;
        }

        Double addedAmount;
        try {
            addedAmount = Double.parseDouble(addedAmountStr);
        } catch (NumberFormatException e) {
            System.out.println("[FAIL] Betting amount cannot String");
            return;
        }

        if (addedAmount > 0) updateBalance(addedAmount); else System.out.println("[FAIL] The amount must be higher than zero");
    }


    private void updateBalance(double balance) {
        this.balance += balance;
        printBalance();
    }


    private void printBalance() {
        System.out.println("Current Balance: " + balance);
    }

    private void rateByGameName() {

        // Insert game name to rate
        System.out.print("Game name (ENTER to skip): ");
        String gameName = scanner.nextLine().trim();
        if (gameName.isEmpty()) {
            System.out.println("[FAIL] Game name cannot be empty");
            return;
        }


        // Build Master Request by player input
        Request request = new Request(Request.Type.RATE_GAME);
        request.put("playerId", playerId);
        request.put("gameName", gameName);

        System.out.print("Stars (1-5, or ENTER to skip): ");
        String starsStr = scanner.nextLine().trim();
        try {
            if (!starsStr.isEmpty()) request.put("stars", Integer.parseInt(starsStr));
            else {
                System.out.println("[FAIL] Stars cannot be empty");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[FAIL] Stars must be int");
            return;
        }


        // Send to Master
        Request response = sendToMaster(request);

        if (!"OK".equals(response.get("status"))) {
            System.out.println("[FAIL] " + response.get("message"));
            return;
        }

        System.out.println("Game rated successfully");
    }


    // TCP operation helpers ----------------------------------------------------------------------------------------------------

    /**
     * Sends to Master newly built Request for current player
     * @param request
     * @return response from Master
     */
    private Request sendToMaster(Request request) {
        try (
                Socket socket = new Socket(masterHost, masterPort);

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        ) {
            oos.flush();
            oos.writeObject(request);
            oos.flush();
            return (Request) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[FAIL] Could not communicate with Master: " + e.getMessage());
            return null;
        }
    }


    // Entry point ----------------------------------------------------------------------------------------------------

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

        System.out.print("Enter your username: ");

        Scanner scan = new Scanner(System.in);
        String playerId = scan.nextLine().trim();

        // Initialize and start Player dummy app
        Player console = new Player(playerId, masterHost, masterPort);
        console.start();
    }
}

