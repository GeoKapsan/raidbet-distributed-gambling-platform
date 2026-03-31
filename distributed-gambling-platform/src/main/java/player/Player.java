package player;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import shared.Request;


public class Player {

    private String username;
    private double balance=0.0;
    private final String masterHost;
    private final int masterPort;
    private final Scanner scanner = new Scanner(System.in);

    public Player (String name, String masterHost, int masterPort){

        username=name;
        this.masterHost=masterHost;
        this.masterPort=masterPort;

        }

    public void start(){
        
        while(true){
            
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> play();
                case "2" -> search();
                case "3" -> checkBalance();
                case "4" -> addBalance();
                case "0" -> {
                    System.out.println("Exiting Player Console.");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }

        }

    }

    private void play() {

        
        System.out.println("Type the game name you want to play");
        String gameName = scanner.nextLine().trim();
        System.out.println("Type the amount you want to bet");
        Double bettingAmount = Double.parseDouble(scanner.nextLine().trim());

        Request request=new Request(Request.Type.PLAY);
        request.put("gameName", gameName);
        request.put("bettingAmount",bettingAmount);

        Request response=sendToMaster(request);

        String status=(String)response.get("status");
        System.out.println(status);
        if (status!="ERROR(wrong hash)"){
            double amountWon=(Double) response.get("amountWon");
            System.out.println("Amount Won: "+ amountWon+"FUN");
            updateBalance(amountWon-bettingAmount);
            checkBalance();
        }

    }

    private void search(){

        

    }


    private void addBalance(){

        System.out.println("Type the amount you want to add");
        Double addedAmount = Double.parseDouble(scanner.nextLine().trim());
        if (addedAmount>0) updateBalance(addedAmount); else System.out.println("The amount must be higher than zero");

    }


    private void checkBalance(){
        System.out.println("Current Balance: "+balance);
    }

    private void updateBalance(double balance){
        this.balance+=balance;
    }

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

    private void printMenu(){
        System.out.println("\n--- "+username+" Console ---");
        System.out.println("1. Play");
        System.out.println("2. Search Games");
        System.out.println("3. Check balance");
        System.out.println("4. Add balance");
        System.out.println("0. Exit");
        System.out.print("Select an option: ");
    }

    public static void main(String[] args) {
        System.out.println("Enter your username");
        Scanner scan = new Scanner(System.in);
        String name = scan.nextLine().trim();
        Player p=new Player(name,"localhost", 5000);
        p.start();

    }

}

