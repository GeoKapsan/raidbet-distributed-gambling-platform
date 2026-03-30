package srg;

import java.io.*;
import java.net.*;
import java.util.*;

import shared.Request;


public class Srg{

    private final int port ;
    private HashMap<String, RandomBuffer> generators = new HashMap<>();
    private HashMap<String, String> hashKeys = new HashMap<>();

    public Master(int port) {
        this.port = port;
    }


    public void start() {
        try (
                ServerSocket serverSocket = new ServerSocket(port)
        ) {

            while (true) {
                Socket workerSocket = serverSocket.accept();
                (new Thread(new SrgHandler(workerSocket, this))).start();
            }
        } catch (IOException e) {

        }
    }

    public synchronized void put(String gameName, String key) {

        generators.put(gameName, new Buffer());
        hashKeys.put(gameName, key);

        new Thread(() -> {
            java.security.SecureRandom random = new java.security.SecureRandom();
            while (true) {
                try {
                    buffer.produce(random.nextInt(Integer.MAX_VALUE));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void remove(String gameName){

        generators.remove(gameName);
        hashKeys.remove(gameName);

    }

    public int getNumber(String gameName){
        return generators.get(gameName).consume();
    }

    public String getHashKey(String gameName){
        return hashKeys.get(gameName);
    }

    private Request sentToMaster(Request request) {

        try (
                Socket socket = new Socket(masterHost, masterPort);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        ) {

            oos.writeObject(request);
            oos.flush();

            return (Request) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[ERROR] Could not communicate with Master: " + e.getMessage());
            return null;
        }
    }


    public static void main(String[] args){
    Srg srg= new Srg();
    srg.start();
    }
}
