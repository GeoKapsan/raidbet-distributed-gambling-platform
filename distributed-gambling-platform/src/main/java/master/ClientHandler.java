package master;

import game.Game;
import shared.Request;
import java.util.*;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket  clientSocket;
    private final Master master;

    public ClientHandler(Socket clientSocket, Master master) {
        this.clientSocket = clientSocket;
        this.master = master;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
                )
        {
            output.flush(); // send header to avoid deadlock

            Request request = (Request) input.readObject(); // receive client's data

            Request response = route(request); // route request and send back response to client

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Master] Client handler error: " + e.getMessage());
        }
    }

    private Request route(Request request) {
        switch (request.getType()) {
            case ADD_GAME, REMOVE_GAME:
                Game game = (Game) request.get("game");
                String gameName = game.getGameName();
                return forwardToWorkerAndGetResult(request, master.getWorkerAddress(gameName));
            case REMOVE_GAME:

            default:
                return new Request(Request.Type.RESPONSE);
        }
    }

    private Request forwardToWorkerAndGetResult(Request request, String workerAddress) {
        String[] parts = workerAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        System.out.println("[Master] Forwarding to worker: " + host + ":" + port);

        try (
                Socket worker = new Socket(host, port);

                ObjectOutputStream output = new ObjectOutputStream(worker.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(worker.getInputStream());
                ) {
            output.flush();
            output.writeObject(request);
            output.flush();
            return (Request) input.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new Request(Request.Type.RESPONSE);
        }

    }

}
