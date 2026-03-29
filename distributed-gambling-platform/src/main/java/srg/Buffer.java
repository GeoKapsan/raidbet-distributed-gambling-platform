package main.java.srg;

import java.util.LinkedList;

public class Buffer {
    
    private final LinkedList<Integer> buffer=new LinkedList<>();
    private final int max_size=10;

    public synchronized void produce(int number) throws InterruptedException {
        while (buffer.size() == MAX_SIZE) {
            wait();
        }
        buffer.addLast(number);
        notifyAll();
    }

    public synchronized int consume() throws InterruptedException {
        while (buffer.isEmpty()) {
            wait();
        }
        int number = buffer.removeFirst();
        notifyAll();
        return number;
    }


}
