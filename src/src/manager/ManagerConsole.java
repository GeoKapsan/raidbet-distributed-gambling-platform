package manager;

import game.Game;
import shared.Request;
import shared.Request.Type;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class ManagerConsole {
    private final String masterHost ;
    private final int masterPort; // αποθηκευουμε διευθυνση και Port του MASTER SEVER
    private final Scanner scanner = new Scanner(System.in);

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


    // ---------------------------------------------------------------
    // Simple manual JSON parser
    // ---------------------------------------------------------------
    static Game parseGameFromJson(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
 
        // Αφαιρούμε whitespace/newlines για ευκολία
        content = content.replaceAll("\\s+", " ").trim();
 
        String gameName    = extractString(content, "GameName");
        String provider    = extractString(content, "ProviderName");
        double stars       = Double.parseDouble(extractValue(content, "Stars"));
        int noOfVotes      = Integer.parseInt(extractValue(content, "NoOfVotes"));
        String logoPath    = extractString(content, "GameLogo");
        double minBet      = Double.parseDouble(extractValue(content, "MinBet"));
        double maxBet      = Double.parseDouble(extractValue(content, "MaxBet"));
        String riskLevel   = extractString(content, "RiskLevel");
        String hashKey     = extractString(content, "HashKey");
 
        return new Game(gameName, provider, stars, noOfVotes,
                        logoPath, minBet, maxBet, riskLevel, hashKey);
    }
 
    // Εξάγει την τιμή ενός string field: "Key": "value"
    private static String extractString(String json, String key) {
        // Ψάχνει το pattern: "Key" : "value"
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx == -1) throw new IllegalArgumentException("Key not found: " + key);
 
        // Πάμε μετά το κλειδί και βρίσκουμε την πρώτη " για την τιμή
        int afterColon = json.indexOf(":", keyIdx + search.length());
        int openQuote  = json.indexOf("\"", afterColon + 1);
        int closeQuote = json.indexOf("\"", openQuote + 1);
 
        return json.substring(openQuote + 1, closeQuote);
    }
 
    // Εξάγει την τιμή ενός numeric/boolean field: "Key": 3
    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx == -1) throw new IllegalArgumentException("Key not found: " + key);
 
        int afterColon = json.indexOf(":", keyIdx + search.length());
 
        // Διαβάζουμε μέχρι το επόμενο , ή }
        int start = afterColon + 1;
        // Παραλειψη spaces
        while (start < json.length() && json.charAt(start) == ' ') start++;
 
        int end = start;
        while (end < json.length()
               && json.charAt(end) != ','
               && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim();
    }

    // --- Manager Operations ---
    //ADD_GAME
        // ADD_GAME
    private void addGame() throws IOException {
        System.out.print("Enter path to game JSON file: ");
        String path = scanner.nextLine().trim();
       // C:\Users\YOUR-NAME-(dimit)\distributed-gambling-platform\src\src\game1.json . ΑΥΤΟ ΕΙΝΑΙ ΤΟ INPUT ΣΤΟ CMD ΓΙΑ ΝΑ ΔΙΑΒΑΣΕΙ ΤΟ JSON

        Game game;
        try {
            game = parseGameFromJson(path);
        } catch (Exception e) {
            System.out.println("Failed to parse JSON: " + e.getMessage());
            return;
        }

        System.out.println("Game parsed successfully:");
        System.out.println("  Name     : " + game.getGameName());
        System.out.println("  Provider : " + game.getProviderName());
        System.out.println("  Risk     : " + game.getRiskLevel());
        System.out.println("  Bet Cat  : " + game.getBettingCategory());
        System.out.println("  Min/Max  : " + game.getMinBet() + " / " + game.getMaxBet());

        // TODO: στειλε το game στον Master οταν ειναι ετοιμη η επικοινωνια
        System.out.println("[TODO] Sending ADD_GAME to Master...");
    }

    public static void main(String[] args) {
    // Βαζουμε dummy host/port γιατι δεν εχουμε Master ακομη
    ManagerConsole console = new ManagerConsole("localhost", 9999);
    console.start();


    }

}
