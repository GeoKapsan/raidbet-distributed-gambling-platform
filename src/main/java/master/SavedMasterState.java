package master;

import java .util.*;

/**
 * A lock object that lives in the Master's waiting room (waitingRoom map).
 * The ClientHandler thread that handled SEARCH puts one of these in the
 * waiting room and then calls waitForResult(), which suspends it via wait().
 * The ClientHandler thread that later handles the REDUCER_CALLBACK calls
 * setResult(), which deposits the final game list and wakes the suspended
 * thread via notify().
 * Both methods are synchronized on 'this' (the SavedMasterState instance),
 * so wait() and notify() operate on the same monitor.
 */
public class SavedMasterState {

    private ArrayList<Object> result;
    private boolean ready = false;

    /**
     * Called by the ClientHandler thread to deposit the Reducer result
     * and stops the suspension of previous ClientHandler thread.
     * @param result Reducer result
     */
    public synchronized void setResult(ArrayList<Object> result) {
        this.result = result;
        ready = true;
        notify(); // wake thread waiting on thread
    }

    /**
     * Called by a ClientHandler thread to suspend itself until setResult() is called
     * or timeout passes.
     * @param timeout time to wait before returning null
     * @return games after Reducer returns result or null if timeout passes
     * @throws InterruptedException
     */
    public synchronized ArrayList<Object> waitForResult(long timeout)
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + timeout;

        while(!ready) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) break; // time out
            wait(remaining);
        }

        return result;
    }
}
