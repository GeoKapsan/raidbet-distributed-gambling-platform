package master;

import reducer.Reducer;
import worker.Worker;
import srg.Srg;

import java.util.*;
import java.io.*;
import java.net.*;


public class Master {

    private final int port;
    private ArrayList<String> workerAddresses;
    private final String srgHost;
    private final int srgPort;

    private int mapIdCounter = 0;

    // Waiting Set/Room: each operation that uses the MapReduce framework registers here & REDUCER_CALLBACK looks up here
    private final HashMap<Integer, SavedMasterState> waitingSet = new HashMap<>();

    public Master(int port, ArrayList<String> workerAddresses, String srgHost, int srgPort) {
        this.port = port;
        this.workerAddresses = workerAddresses;
        this.srgHost = srgHost;
        this.srgPort = srgPort;
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
    public SavedMasterState getMasterState(int mapId) {
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

    /**
     * Returns address of worker responsible for game 'gameName'
     */
    public String getWorkerAddress(String gameName) {
        return workerAddresses.get(getWorkerIndex(gameName));
    }

    public String getSrgHost(){
        return srgHost;
    }

    public int getSrgPort(){
        return srgPort;
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

        int workerCount    = Integer.parseInt(config.getProperty("worker.count",  "1"));

        String reducerHost = config.getProperty("reducer.host", "localhost");
        int reducerPort    = Integer.parseInt(config.getProperty("reducer.port"));

        String srgHost     = config.getProperty("srg.host", "localhost");
        int    srgPort     = Integer.parseInt(config.getProperty("srg.port"));

        ArrayList<String> workers = new ArrayList<>();
        for (int i = 0; i < workerCount; i++) {
            workers.add(config.getProperty("worker." + i));
        }

        // Initialize and start Workers
        for (int i = 0; i < workerCount; i++) {

            int workerPort_i = Integer.parseInt(config.getProperty("worker." + i).split(":")[1]);

            Worker worker = new Worker(workerPort_i, reducerHost, reducerPort, srgHost, srgPort);
            new Thread(() -> worker.start()).start();
        }

        // Initialize and start Reducer
        Reducer reducer = new Reducer(reducerPort, masterHost, masterPort);
        new Thread(() -> reducer.start()).start();

        // Initialize and start SRG
        Srg srg = new Srg(srgPort, masterHost, masterPort);
        new Thread(() -> srg.start()).start();

        new Master(masterPort, workers, srgHost, srgPort).start();

    }
}
