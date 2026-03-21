package manager;

import game.Game;
import shared.Request;
import shared.Request.Type;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.ArrayList ;
import java.util.List ;


import org.json.simple.JSONObject ;
import org.json.simple.parser.JSONParser; // Απαραίτητο για τον parser
import org.json.simple.parser.ParseException;

public class ManagerConsole {
    private final String masterHost ;
    private final int masterPort; // αποθηκευουμε διευθυνση και Port του MASTER SEVER
    private final Scanner scanner = new Scanner(System.in);

    // λιστα για τα loaded games
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
        }
    }

    //addgame()

    private void addGame() {
        System.out.print("Enter path to the folder containing game JSON files: ");
        String folderPath = scanner.nextLine().trim();

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid directory path!");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null || files.length == 0) {
            System.out.println("No JSON files found in: " + folder.getAbsolutePath());
            return;
        }

        // Φτιάχνουμε τον Parser ΜΙΑ φορά έξω από τη λούπα για καλύτερη απόδοση
        JSONParser parser = new JSONParser();

        for (File file : files) {
            // Using try-with-resources to automatically close the FileReader
            try (FileReader reader = new FileReader(file)) {

                // 1. Parse the file directly into a JSONObject
                JSONObject jsonObject = (JSONObject) parser.parse(reader);

                // 2. Extract String values
                String gameName = (String) jsonObject.get("GameName");
                String providerName = (String) jsonObject.get("ProviderName");
                String logoPath = (String) jsonObject.get("GameLogo");
                String riskLevel = (String) jsonObject.get("RiskLevel");
                String hashKey = (String) jsonObject.get("HashKey");

                // 3. Extract Number values safely
                double stars = ((Number) jsonObject.get("Stars")).doubleValue();
                int noOfVotes = ((Number) jsonObject.get("NoOfVotes")).intValue();
                double minBet = ((Number) jsonObject.get("MinBet")).doubleValue();
                double maxBet = ((Number) jsonObject.get("MaxBet")).doubleValue();

                // 4. Instantiate your Game object
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

                // Προσθήκη στη λίστα του Manager
                loadedGames.add(parsedGame);


                System.out.println("Game parsed successfully:");
                System.out.println("  Name     : " + parsedGame.getGameName());
                System.out.println("  Provider : " + parsedGame.getProviderName());
                System.out.println("  Risk     : " + parsedGame.getRiskLevel());
                System.out.println("  Bet Cat  : " + parsedGame.getBettingCategory());
                System.out.println("  Min/Max  : " + parsedGame.getMinBet() + " / " + parsedGame.getMaxBet());
                System.out.println("-----------------------------------");

            } catch (Exception e) {
                System.out.println("Failed to parse file " + file.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("Total games currently loaded: " + loadedGames.size());
    }

    public static void main(String[] args) {
    // Βαζουμε dummy host/port γιατι δεν εχουμε Master ακομη
    ManagerConsole console = new ManagerConsole("localhost", 9999);
    console.start();


    }

}
