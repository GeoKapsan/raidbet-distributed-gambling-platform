package manager;

import game.Game;
import shared.Request;
import shared.Request.Type;

import org.json.JSONObject; // Απαραίτητο για το parsing

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ManagerConsole {
    private final String masterHost ;
    private final int masterPort; // αποθηκευουμε διευθυνση και Port του MASTER SEVER
    private final Scanner scanner = new Scanner(System.in);

    //λιστα για τα loaded games 
    private List<Game> loadedGames = new ArrayList<>();

    public ManagerConsole(String masterHost , int masterPort){
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }


    private void printMenu(){
        System.out.println("\n--- Manager Console ---");
        System.out.println("1. Add Game");
        System.out.println("2. Remove Game");
        System.out.println("3. Change Game Risk Level");
        System.out.println("0. Exit");
        System.out.print("Select an option: ");
    }


    public void start() {

        System.out.println("==Manager Console==");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> addGame();
                    //case "2" -> removeGame();
                    //case "3" -> changeRiskLevel();
                    case "0" -> {
                        System.out.println("Exiting Manager Console.");
                        return;
                    }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            } catch (IOException e) {
                System.out.println("Error communicating with Master Server: " + e.getMessage());
            }
        }
    }

    private void loadGamesFromFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (listOfFiles == null || listOfFiles.length == 0) {
            System.out.println("Δεν βρέθηκαν αρχεία JSON στον φάκελο: " + folderPath);
            return;
        }

        int successCount = 0;
        for (File file : listOfFiles) {
            try {
                Game game = parseGameFromJson(file.getAbsolutePath());
                loadedGames.add(game);
                successCount++;
            } catch (Exception e) {
                System.err.println("Σφάλμα στο αρχείο " + file.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("Επιτυχής φόρτωση " + successCount + " παιχνιδιών από τον φάκελο.");
    }

    private static Game parseGameFromJson(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONObject json = new JSONObject(content);

        // Αντιστοίχιση των πεδίων του JSON με τον constructor της Game
        return new Game(
            json.getString("GameName"),
            json.getString("ProviderName"),
            json.getDouble("Stars"),
            json.getInt("NoOfVotes"),
            json.getString("GameLogo"),
            json.getDouble("MinBet"),
            json.getDouble("MaxBet"),
            json.getString("RiskLevel"),
            json.getString("HashKey")
        );
    }

   

    // --- Manager Operations ---
    //ADD_GAME
    private void addGame() throws IOException {
        System.out.print("Enter path to the folder containing game JSON files: ");
        String inputPath = scanner.nextLine().trim();

        // Χρήση File για κανονικοποίηση του path
        File folder = new File(inputPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid directory path!");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (files == null || files.length == 0) {
            System.out.println("No JSON files found in: " + folder.getAbsolutePath());
            return;
        }

        List<Game> gameList = new ArrayList<>();

        for (File file : files) {
            try {
                // Χρησιμοποιούμε τη βιβλιοθήκη org.json για το parsing
                Game game = parseGameFromJson(file.getAbsolutePath());
                gameList.add(game);

                System.out.println("Game parsed successfully:");
                System.out.println("  Name     : " + game.getGameName());
                System.out.println("  Provider : " + game.getProviderName());
                System.out.println("  Risk     : " + game.getRiskLevel());
                System.out.println("  Bet Cat  : " + game.getBettingCategory()); // Υπολογίζεται αυτόματα στην Game
                System.out.println("  Min/Max  : " + game.getMinBet() + " / " + game.getMaxBet());
                System.out.println("-----------------------------------");

            } catch (Exception e) {
                System.out.println("Failed to parse file " + file.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("Total games loaded in memory: " + gameList.size());
    }
    
    public static void main(String[] args) {
    // Βαζουμε dummy host/port γιατι δεν εχουμε Master ακομη
    ManagerConsole console = new ManagerConsole("localhost", 9999);
    console.start();


    }

}
