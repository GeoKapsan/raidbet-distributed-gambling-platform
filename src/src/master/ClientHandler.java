package master;

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

            Request request = (Request) input.readObject();

            Request response = route(request);
            output.writeObject(response);
            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Master] Client handler error: " + e.getMessage());
        }
    }

    private Request route(Request request) {
        switch (request.getType()) {
            case ADD_GAME:

<<<<<<< HEAD
            default:
=======
>>>>>>> 0a8ebcd6ca88c4334af7d306ce0f7e2b7e3d5de7
        }
    }

    private Request forwardToWorker();


}
