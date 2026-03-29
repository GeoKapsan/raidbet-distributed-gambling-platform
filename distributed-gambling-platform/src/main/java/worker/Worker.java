package worker;

import game.Game;
import shared.Request;
import java.io.*;
import java.net.*;
import java.util.*;

public class Worker {

    private final String host;
    private final int port;
    private final String reducerHost;
    private final int reducerPort;
    private final HashMap<String, Game> games = new HashMap<>();
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
                ServerSocket serverSocket = new ServerSocket("",port);
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

    public synchronized void addGame(Game game) {
        games.put(game.getGameName(), game);
        System.out.println("[Worker:" + port + "] Added game " + game.getGameName());
    }

    public synchronized void removeGame(String gameName) {
        Game game = games.get(gameName);
        if (games.get(gameName) != null) {
            game.setActive(false);
            System.out.println("[Worker:" + port + "] Removed game " + game.getGameName());
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

    public int getSrgPort(){
        return srgPort;
  

    // Entry point ----------------------------------------------------------------------------------------------------
      
    public static void main(String[] args) {



    }
}
