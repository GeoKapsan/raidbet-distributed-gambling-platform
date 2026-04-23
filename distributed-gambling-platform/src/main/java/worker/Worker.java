package worker;

import game.Game;
import master.Master;
import java.io.*;
import java.net.*;
import java.util.*;

public class Worker {

    private final int port;
    private final String reducerHost;
    private final int reducerPort;
    private final HashMap<String, Game> games = new HashMap<>();
    private final HashMap<String, Double> gamesProfit = new HashMap<>();
    private final HashMap<String, Double> playersProfit = new HashMap<>();
    private final String srgHost;
    private final int srgPort;

    public Worker(int port, String reducerHost, int reducerPort, String srgHost, int srgPort) {
        this.port = port;
        this.reducerHost = reducerHost;
        this.reducerPort = reducerPort;
        this.srgHost = srgHost;
        this.srgPort = srgPort;
    }

    public void start() {
        try (
                ServerSocket serverSocket = new ServerSocket(port);
                ) {
            System.out.println("Worker server listening on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Worker:" + port + "] New connection from " + clientSocket.getInetAddress());

                (new Thread(new WorkerHandler(clientSocket, this))).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Worker's game operations ----------------------------------------------------------------------------------------------------

    /**
     * Adds game to Worker's game list.
     * If the game already exists, it is active then it returns false and ignores the incoming game to add.
     * If the game already exists, it is not active, then it activates the game with that name.
     * @param game the game to add
     * @return true if the game was added successfully, false if it wasn't added
     */
    public synchronized boolean addGame(Game game) {
        if (games.containsKey(game.getGameName())) {
            if (games.get(game.getGameName()).isActive())
                return false;
            games.get(game.getGameName()).setActive(true);
        } else {
            games.put(game.getGameName(), game);
        }
        System.out.println("[Worker:" + port + "] Added game " + game.getGameName());
        return true;
    }

    /**
     * Updates games profit for specific game in the Worker's game list
     * @param gameName
     * @param newProfit
     */
    public synchronized void updateGameProfit(String gameName, double newProfit) {

        if (gamesProfit.containsKey(gameName)) {
            Double oldProfit = gamesProfit.get(gameName);
            gamesProfit.put(gameName, oldProfit + newProfit);
        } else {
            gamesProfit.put(gameName, newProfit);
        }
    }

    /**
     * Updates player profit for specific player Id in the Worker's players list
     * @param playerID
     * @param newProfit
     */
    public synchronized void updatePlayerProfit(String playerID, double newProfit) {

        if (playersProfit.containsKey(playerID)) {
            Double oldProfit = playersProfit.get(playerID);
            playersProfit.put(playerID, oldProfit + newProfit);
        } else {
            playersProfit.put(playerID, newProfit);
        }
    }

    public double getPlayerProfit(String playerID) {
        if (playersProfit.containsKey(playerID)) return playersProfit.get(playerID);
        return Double.NaN;
    }

    public double getGameProfit(String gameName) {
        if (gamesProfit.containsKey(gameName)) return gamesProfit.get(gameName);
        return Double.NaN;
    }

    /**
     * Removes game from Worker's game list.
     * If game does not exist it returns false.
     * If the game is removed successfully it returns true.
     * @param gameName
     * @return
     */
    public synchronized boolean removeGame(String gameName) {
        Game game = games.get(gameName);
        if (game != null) {
            game.setActive(false);
            System.out.println("[Worker:" + port + "] Removed game " + game.getGameName());
            return true;
        }
        else  {
            System.out.println("[Worker:" + port + "] Failed to remove game '" +  gameName + "'" );
            return false;
        }
    }

    public synchronized Game getGame(String gameName) {
        return games.get(gameName);
    }

    public synchronized ArrayList<Game> getAllGames() {
        return new ArrayList<>(games.values());
    }


    // WorkerHandler getters ----------------------------------------------------------------------------------------------------

    public int getPort() {
        return port;
    }

    public String getReducerHostAndPort() {
        return reducerHost + ":" + reducerPort;
    }

    public String getSrgHost(){
        return srgHost;
    }

    public int getSrgPort() {
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

        String reducerHost = config.getProperty("reducer.host", "localhost");
        int reducerPort    = Integer.parseInt(config.getProperty("reducer.port"));

        String srgHost     = config.getProperty("srg.host", "localhost");
        int    srgPort     = Integer.parseInt(config.getProperty("srg.port"));

        // Initialize and start Worker
        int workerPort;
        try {
            workerPort = Integer.parseInt(args[0]);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            workerPort = Integer.parseInt(config.getProperty("worker.port"));
        }
        
        Worker worker = new Worker(workerPort, reducerHost, reducerPort, srgHost, srgPort);
        worker.start();
    }

}
