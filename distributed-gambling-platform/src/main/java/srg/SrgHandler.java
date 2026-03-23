package srg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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

            Request response = route(request); // route request and send back response to client

            output.writeObject(response);

            output.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Srg] Srg handler error: " + e.getMessage());
        }
    }



    
}