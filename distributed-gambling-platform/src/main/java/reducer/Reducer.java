package reducer;

import game.Game;
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
    private final Map<Integer, Integer> receivedCounts = new HashMap<>();

    // Accumulated game lists from Workers, keyed by mapId
    private final HashMap<Integer, ArrayList<String[]>> collectedGames = new HashMap<>();

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
                System.out.println("[Reducer:" + port + "] New connection from " + clientSocket.getInetAddress());

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
        return expectedCounts.containsKey(mapId) || receivedCounts.containsKey(mapId) || collectedGames.containsKey(mapId);
    }

    public synchronized void registerMapReduce(int mapId, int noOfWorkers) {
        expectedCounts.put(mapId, noOfWorkers);

        collectedGames.put(mapId, new ArrayList<>());

        receivedCounts.put(mapId, 1);
    }

    public synchronized boolean collect(int mapId, ArrayList<String[]> games) {
        receivedCounts.put(mapId, receivedCounts.get(mapId) + 1);

        for (String[] game : games)
            collectedGames.get(mapId).add(game);

        return expectedCounts.get(mapId) == receivedCounts.get(mapId);
    }

    public synchronized ArrayList<String[]> getCollectedGames(int mapId) {
        return new ArrayList<String[]>(collectedGames.get(mapId));
    }

    public synchronized ArrayList<String> reduce(int mapId, ArrayList<String[]> games) {
        ArrayList<String> result = new ArrayList<>();

        for (String[] game : games)
            result.add(game[1]);

        return result;
    }

    public synchronized void cleanup(int mapId) {
        expectedCounts.remove(mapId);
        receivedCounts.remove(mapId);
        collectedGames.remove(mapId);
    }

}
