package master;

import java.util.*;
import java.io.*;
import java.net.*;

public class Master {

    private final int port;
    private ArrayList<String> workerAddresses;

    public Master(int port, int noOfWorkers) {
        this.port = port;
        this.workerAddresses = new ArrayList<String>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Master server listening on port " + port);
            System.out.println("Workers' addresses: " + workerAddresses);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from " + clientSocket.getInetAddress());

                //
            }
        } catch (IOException e) {

        }
    }

}
