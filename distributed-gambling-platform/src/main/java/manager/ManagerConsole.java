package manager;

import game.Game;
import shared.Request;

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
        System.out.println("4. List Games");
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
                case "2" -> removeGame();
                case "3" -> changeRiskLevel();
                case "4" -> listGame();
                case "0" -> {
                    System.out.println("Exiting Manager Console.");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    //listGames()

    public void listGame(){
        System.out.println("fetching the game from  the network");
        Request request = new Request(Request.Type.SHOW_GAMES);
        Request response = sentToMaster(request);

        if (response == null){
            System.out.println("[FAIL] No response from Master");
            return ;

        }
        String status = (String) response.get("status");
        if ("OK".equalsIgnoreCase(status)){
            List<Game> networkGames = (List<Game>) response.get("games"); // o Master tha epistrefei lista logika

            if (networkGames == null || networkGames.isEmpty()){
                System.out.println("no games loaded ");
                return ;

            }

            System.out.println("\n--- Distributed Games (" + networkGames.size() + ") ---");
            for (int i = 0; i < networkGames.size(); i++) {
                Game g = networkGames.get(i);
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





    //anoigei sockets gia epikoinonia me masterbaiter(rapth)
    private Request sentToMaster(Request request) {

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
                double stars = ((Number) jsonObject.get("Stars")).doubleValue();
                int noOfVotes = ((Number) jsonObject.get("NoOfVotes")).intValue();
                String logoPath = (String) jsonObject.get("GameLogo");
                double minBet = ((Number) jsonObject.get("MinBet")).doubleValue();
                double maxBet = ((Number) jsonObject.get("MaxBet")).doubleValue();
                String riskLevel = (String) jsonObject.get("RiskLevel");
                String hashKey = (String) jsonObject.get("HashKey");



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


                //dimiourgia toy request
                Request request = new Request(Request.Type.ADD_GAME);
                request.put("game" , parsedGame);

                //apostoli ston grandmasterbaiter(rapth) meso tcp
                System.out.println("Sending ADD_GAME request for : "+ gameName);
                Request response = sentToMaster(request);

                if (response == null){
                    System.out.println("fail"+gameName);

                    continue;
                }

                String status = (String) response.get("status");
                String message = (String) response.get("message");

                //prothiki ston master
                if ("OK".equalsIgnoreCase(status)) {
                    System.out.println("[SUCCESS] Game added: " + gameName + (message != null ? " - " + message : ""));
                } else {
                    System.out.println("[FAIL] Could not add game: " + gameName + (message != null ? " - " + message : ""));
                }





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
    }

    //remove game
    private void removeGame() {
        System.out.print("Enter the name of the game to remove: ");
        String gameName = scanner.nextLine().trim();

        if (gameName.isEmpty()) {
            System.out.println("Game name cannot be empty.");
            return;
        }

        // kanw game object ta ypoloipa paidia dummy
        Game gameToRemove = new Game(gameName, "", 0, 0, "", 0, 0, "", "");

        // ftiaxno to request to vazw sto payload
        Request request = new Request(Request.Type.REMOVE_GAME);
        request.put("game", gameToRemove);

        //stelnw se master
        System.out.println("Sending REMOVE_GAME request for: " + gameName);
        Request response = sentToMaster(request);

        //elegxw response
        if (response == null) {
            System.out.println("[FAIL] No response from Master.");
            return;
        }

        String status  = (String) response.get("status");
        String message = (String) response.get("message");

        if ("OK".equalsIgnoreCase(status)) {
            System.out.println("[SUCCESS] Game removed: " + gameName
                    + (message != null ? " - " + message : ""));
        } else {
            System.out.println("[FAIL] Could not remove game: " + gameName
                    + (message != null ? " - " + message : ""));
        }
    }




    //change risk level
    private void changeRiskLevel() {
        System.out.print("Enter the name of the game: ");
        String gameName = scanner.nextLine().trim();

        if (gameName.isEmpty()) {
            System.out.println("Game name cannot be empty.");
            return;
        }

        System.out.print("Enter new risk level (Low / Medium / High): ");
        String newRiskLevel = scanner.nextLine().trim();

        if (newRiskLevel.isEmpty()) {
            System.out.println("Risk level cannot be empty.");
            return;
        }

        // ftiaxnw Game object me to onoma kai to neo risk level
        Game gameToUpdate = new Game(gameName, "", 0, 0, "", 0, 0, newRiskLevel, "");

        // request
        Request request = new Request(Request.Type.CHANGE_RISK);
        request.put("game", gameToUpdate);

        // ston rapth mesw TCP
        System.out.println("Sending CHANGE_RISK request for: " + gameName + " → " + newRiskLevel);
        Request response = sentToMaster(request);

        // elegxw response
        if (response == null) {
            System.out.println("[FAIL] No response from Master.");
            return;
        }

        String status  = (String) response.get("status");
        String message = (String) response.get("message");

        if ("OK".equalsIgnoreCase(status)) {

            System.out.println("[SUCCESS] Risk level changed for: " + gameName
                    + (message != null ? " - " + message : ""));
        } else {
            System.out.println("[FAIL] Could not change risk level for: " + gameName
                    + (message != null ? " - " + message : ""));
        }
    }



    public static void main(String[] args) {
        // Βαζουμε dummy host/port γιατι δεν εχουμε Master ακομη
        ManagerConsole console = new ManagerConsole("localhost", 5000);
        console.start();


    }
}


