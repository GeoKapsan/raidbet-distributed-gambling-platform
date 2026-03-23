package srg;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Srg{


    public void start() {
        try (
                ServerSocket serverSocket = new ServerSocket(port)
        ) {
            System.out.println("Master server listening on port " + port);
            System.out.println("Workers' addresses: " + workerAddresses);

            while (true) {
                Socket workerSocket = serverSocket.accept();
                (new Thread(new SrgHandler(workerSocket, this))).start();
            }
        } catch (IOException e) {

        }
    }
    public void run(){

    }

    public static void main(String[] args){
    Srg srg= new Srg();
    srg.start();
    srg.run();    
}
}
