package manager;


import game.Game;
import shared.Request;
import shared.Request.Type;

import org.json.simple.JSONObject;// Απαραίτητο για το parsing

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




    // --- Manager Operations ---
    //ADD_GAME


    public static void main(String[] args) {
        // Βαζουμε dummy host/port γιατι δεν εχουμε Master ακομη
        ManagerConsole console = new ManagerConsole("localhost", 9999);
        console.start();


    }

}
