package reducer;

import game.Game;
import master.Master;
import shared.Request;

import java.net.*;
import java.util.*;
import java.io.*;


public class Reducer {

    private final int port;
    private final String masterHost;
    private final int masterPort;
    private final int noOfWorkers;

    // How many map results have arrived so far for each mapId
    private final HashMap<Integer, Integer> receivedCounts = new HashMap<>();

    // Accumulated game lists from Workers
    private final HashMap<Integer, ArrayList<String>> collectedResults = new HashMap<Integer, ArrayList<String>>();

    public Reducer(int port, String masterHost, int masterPort, int noOfWorkers) {
        this.port = port;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.noOfWorkers = noOfWorkers;
    }

    public void start() {
        try (
                ServerSocket serverSocket = new ServerSocket(port);
        ) {
            System.out.println("Reducer server listening on port " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Reducer] New connection from " + clientSocket.getInetAddress());

                (new Thread(new ReducerHandler(clientSocket, this))).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized int getNoOfWorkers() {
        return noOfWorkers;
    }

    public String getMasterHostAndPort() {
        return masterHost + ":" + masterPort;
    }


    // Reducer state operations

    public synchronized boolean mapIdRegistered(Integer mapId) {
        return receivedCounts.containsKey(mapId) || collectedResults.containsKey(mapId);
    }

    public synchronized void registerMapReduce(int mapId) {
        receivedCounts.put(mapId, 0);
        collectedResults.put(mapId, new ArrayList<String>());
    }

    public synchronized boolean collect(int mapId, ArrayList<String> results) {
        receivedCounts.put(mapId, receivedCounts.get(mapId) + 1);

        collectedResults.get(mapId).addAll(results);

        return noOfWorkers == receivedCounts.get(mapId);
    }

    public ArrayList<String> getCollectedResults(int mapId) {
        return collectedResults.get(mapId);
    }



    public synchronized void cleanup(int mapId) {
        receivedCounts.remove(mapId);

        // remove games for mapId after reduce operation
        collectedResults.remove(mapId);
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

        int reducerPort    = Integer.parseInt(config.getProperty("reducer.port"));

        int workerCount    = Integer.parseInt(config.getProperty("worker.count",  "3"));

        // Initialize and start Reducer
        Reducer reducer = new Reducer(reducerPort, masterHost, masterPort, workerCount);
        reducer.start();
    }

}
