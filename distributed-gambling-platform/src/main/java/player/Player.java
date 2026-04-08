package player;

import java.io.*;
import java.net.Socket;
import java.util.*;

import manager.ManagerConsole;
import shared.Request;
import game.Game;


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
                case "5": checkBalance(); break;
                case "0": {
                    System.out.println("Exiting Player Console.");
                    return;
                }
                default:
                    System.out.println("Invalid option. Please try again.");
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
        //System.out.println("5. Check balance");
        System.out.println("0. Exit");
        System.out.print("Select an option: ");
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

    private void play() {
        System.out.print("Game name (ENTER to skip): ");
        String gameName = scanner.nextLine().trim();
        if (gameName.isEmpty()) {
            System.out.println("[FAIL] Game name cannot be empty");
            return;
        }

        System.out.print("Betting amount (ENTER to skip): ");
        Double bettingAmount;
        try {
            bettingAmount = Double.parseDouble(scanner.nextLine().trim());
        } catch (NullPointerException e) {
            System.out.println("[FAIL] Betting amount cannot be null");
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
            System.out.println("[FAIL] Could not play game " + gameName);
            System.out.println(status);
            return;
        }

        double amountWon = (Double) response.get("amountWon");
        System.out.println("Amount Won: "+ amountWon + "FUN");

        // Update balance for current player
        updateBalance(amountWon - bettingAmount);

        checkBalance();
    }

    private String checkBettingAmountValidity(double amount) {
        if (amount <= 0.0) return "[FAIL] Cannot insert negative amount";
        if (amount > balance) return "[FAIL] Not enough balance";
        return "OK";
    }

    private void search() {

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
        if (!starsStr.isEmpty()) request.put("stars", Integer.parseInt(starsStr));

        // Send to Master
        Request response = sendToMaster(request);

        if ("ERROR".equals(response.get("status"))) {
            System.out.println("Error: " + response.get("message"));
            return;
        }

        ArrayList<String> gameNames = (ArrayList<String>) response.get("gameNames");
        if (gameNames == null || gameNames.isEmpty()) {
            System.out.println("No games match your filters.");
            return;
        }

        System.out.println("\n── Matching games ──────────────────────────");
        for (String g : gameNames) {
            System.out.println(g);
            System.out.println("-----------------------------------");
        }
        System.out.println("────────────────────────────────────────");
    }

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

    private void addBalance() {
        System.out.print("Amount to add (ENTER to skip): ");
        Double addedAmount;
        try {
            addedAmount = Double.parseDouble(scanner.nextLine().trim());
        } catch (NullPointerException e) {
            System.out.println("[FAIL] Amount cannot be null");
            return;
        }

        if (addedAmount > 0) updateBalance(addedAmount); else System.out.println("[FAIL] The amount must be higher than zero");

    }


    private void updateBalance(double balance) {
        this.balance += balance;
        checkBalance();
    }


    private void checkBalance() {
        System.out.println("Current Balance: " + balance);
    }



    public static void main(String[] args) {
        System.out.print("Enter your username: ");

        Scanner scan = new Scanner(System.in);
        String playerId = scan.nextLine().trim();

        Player console = new Player(playerId, "localhost", 5001);
        console.start();
    }
}

