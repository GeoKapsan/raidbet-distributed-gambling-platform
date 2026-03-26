package srg;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;


public class Srg{

    private final int port = 0;
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

    public static void main(String[] args){
    Srg srg= new Srg();
    srg.start();
    }
}
