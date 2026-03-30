package srg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;

import shared.Request;

public class SrgHandler implements Runnable{
    
    private Srg srg;
    private Socket clientSocket;
    
    public SrgHandler(Srg srg, Socket clientSocket){

        this.srg = srg;
        this.clientSocket = clientSocket;

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
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.err.println("[Srg] Srg handler error: " + e.getMessage());
        }
    }

    private Request handle(Request request) throws InterruptedException {

        String gameName = (String) request.get("gameName");
        Request response = new Request(Request.Type.RESPONSE);

        switch (request.getType()) {

            case ADD_GAME:

                srg.put(gameName, (String) request.get("hashKey"));
                response.put("status", "OK");
                response.put("message", "Game" + gameName + " added successfully.");
                break;

            case REMOVE_GAME:

                srg.remove(gameName);
                response.put("status", "OK");
                response.put("message", "Game" + gameName + " removed successfully.");
                break;

            case GIVE_NUMBER:

                int number= 0;
                try {
                    number = srg.getNumber(gameName);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                response.put("status", "OK");
                response.put("number", number);
                response.put("hashedNumber", sha256(number + srg.getHashKey(gameName)));
                break;

            default:
                break;
                
        }

        return response;
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