package worker;

import java.net.Socket;

public class WorkerHandler implements Runnable {

    private Socket clientSocket;
    private Worker worker;

    public WorkerHandler(Socket clientSocket, Worker worker) {
        this.clientSocket = clientSocket;
        this.worker = worker;
    }

    public
}
