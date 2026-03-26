package srg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;

import game.Game;
import shared.Request;

public class SrgHandler{
    
    private Srg srg;
    private Socket workerSocket;
    
    public void SrgHandler(Srg srg, Socket workerSocket){

        this.srg=srg;
        this.socket=workerSocket;

    }

    public void run() {
        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
                )
        {
            output.flush(); // send header to avoid deadlock

            Request request = (Request) input.readObject(); // receive client's data

            Request response = handle(request); // route request and send back response to client

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Srg] Srg handler error: " + e.getMessage());
        }
    }

    private Request handleRequest(Request request) {
        /*
        Handles...
         */

        String gameName=request.get(gameName);

        switch (request.getType()) {

            case ADD_GAME:

                srg.put(gameName, request.get("hashKey"));
                response.put("message", "Game" + gameName + " added successfully.");
                break;

            case REMOVE_GAME:

                srg.remove(gameName);
                response.put("message", "Game" + gameName + " removed successfully.");
                break;

            case GIVE_NUMBER:

                int number=srg.getNumber(gameName);
                response.put("number", number);
                response.put("hashed_number", sha256(number+srg.getHashKey(gameName)));
                break;

            default:
                return new Request(Request.Type.RESPONSE);
                
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    
}