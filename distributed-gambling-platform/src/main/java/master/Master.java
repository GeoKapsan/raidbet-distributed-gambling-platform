package master;

import java.util.*;
import java.io.*;
import java.net.*;

public class Master {

    private final int port;
    private ArrayList<String> workerAddresses;

    private int mapIdCounter = 0;

    // Waiting Set/Room: each operation that uses the MapReduce framework registers here & REDUCER_CALLBACK looks up here
    private final HashMap<Integer, SavedMasterState> waitingSet = new HashMap<>();

    public Master(int port, ArrayList<String> workerAddresses) {
        this.port = port;
        this.workerAddresses = workerAddresses;
    }

    public void start() {
        try (
                ServerSocket serverSocket = new ServerSocket(port)
        ) {
            System.out.println("Master server listening on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Master] New connection from " + clientSocket.getInetAddress());

                (new Thread(new ClientHandler(clientSocket, this))).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Saving the Master state (ClientHandler thread) operations ----------------------------------------------------------------------------------------------------

    /**
     * Generate unique map id
     * @return the new map id
     */
    public synchronized int generateMapId() {
        return mapIdCounter++;
    }

    /**
     * Register the state of the current ClientHandler thread so REDUCER_CALLBACK can find it later
     * @param mapId the map id to register
     * @param state the current ClientHandler thread that will suspend
     */
    public synchronized void registerMapReduceOperation(int mapId, SavedMasterState state) {
        waitingSet.put(mapId, state);
    }

    /**
     * Returns the state of the ClientHandler thread for specific map id
     * @param mapId the map id
     * @return the state of the ClientHandler thread
     */
    public synchronized SavedMasterState getMasterState(int mapId) {
        return waitingSet.get(mapId);
    }


    /**
     * Removes the state for the ClientThread for the specific map id
     * @param mapId the map id
     */
    public synchronized void removeSavedMasterState(int mapId) {
        waitingSet.remove(mapId);
    }


    // Routing operations ----------------------------------------------------------------------------------------------------

    public ArrayList<String> getAllWorkerAddresses() {
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


    // Entry point ----------------------------------------------------------------------------------------------------

    public static void main(String[] args) {



    }

}
