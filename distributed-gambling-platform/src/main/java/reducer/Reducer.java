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

    // How many map results to expect before triggering reduce
    private final HashMap<Integer, Integer> expectedCounts = new HashMap<>();

    // How many map results have arrived so far for each mapId
    private final HashMap<Integer, Integer> receivedCounts = new HashMap<>();

    // Accumulated game lists from Workers
    private final ArrayList<String[]> collectedGames = new ArrayList<>();

    public Reducer(int port, String masterHost, int masterPort) {
        this.port = port;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
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

    public String getMasterHostAndPort() {
        return masterHost + ":" + masterPort;
    }


    // Reducer state operations

    public synchronized boolean mapIdRegistered(Integer mapId) {
        return expectedCounts.containsKey(mapId) || receivedCounts.containsKey(mapId);
    }

    public synchronized void registerMapReduce(int mapId, int noOfWorkers) {
        expectedCounts.put(mapId, noOfWorkers);
        receivedCounts.put(mapId, 1);
    }

    public synchronized boolean collect(int mapId, ArrayList<String[]> games) {
        receivedCounts.put(mapId, receivedCounts.get(mapId) + 1);

        collectedGames.addAll(games);

        return expectedCounts.get(mapId) == receivedCounts.get(mapId);
    }

    public synchronized ArrayList<String[]> getCollectedGames() {
        return collectedGames;
    }

    public synchronized ArrayList<String> reduce(int mapId, ArrayList<String[]> games) {
        ArrayList<String> result = new ArrayList<>();

        for (String[] game : games) {
            if (Integer.parseInt(game[0]) != mapId) continue;
            result.add(game[1]);
            games.remove(game);
        }

        return result;
    }

    public synchronized void cleanup(int mapId) {
        expectedCounts.remove(mapId);
        receivedCounts.remove(mapId);
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

        // Initialize and start Reducer
        Reducer reducer = new Reducer(reducerPort, masterHost, masterPort);
        reducer.start();
    }

}
