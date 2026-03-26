package master;

import java.util.*;
import java.io.*;
import java.net.*;

public class Master {

    private final int port;
    private ArrayList<String> workerAddresses;
    private final String srgHost;
    private final int srgPort;

    public Master(int port, int noOfWorkers, String srgHost, int srgPort) {
        this.port = port;
        this.workerAddresses = new ArrayList<String>();
        this.srgHost=srgHost;
        this.srgPort=srgPort;
    }

    public void start() {
        try (
                ServerSocket serverSocket = new ServerSocket(port)
        ) {
            System.out.println("Master server listening on port " + port);
            System.out.println("Workers' addresses: " + workerAddresses);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Master] New connection from " + clientSocket.getInetAddress());

                (new Thread(new ClientHandler(clientSocket, this))).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getWorkerAddresses() {
        return workerAddresses;
    }

    private int getWorkerIndex(String gameName) {
        return Math.abs(gameName.hashCode() % workerAddresses.size()); // H(GameName) mod #(workers) returns worker that has this game
    }

    public String getWorkerAddress(String gameName) {
        /*
         * Returns address of worker responsible for game 'gameName'
         */
        return workerAddresses.get(getWorkerIndex(gameName));
    }
    //thn main thn evale o mpampas na dei oti ola entajei
    public static void main(String[] args) {
        Master master = new Master(5000, 10);
        master.start();
    }

    public String getSrgHost(){
        return srgHost;
    }

    public int getSrgPort(){
        return srgPort;
    }
}
