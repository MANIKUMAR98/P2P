package src;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TerminateHandler implements Runnable {
    private int interval;
    private PeerManager peerManager;
    private Random rand = new Random();
    private ScheduledFuture<?> job = null;
    private ScheduledExecutorService scheduler = null;

    TerminateHandler(PeerManager peerManager) {
        this.peerManager = peerManager;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startJob(int timeinterval) {
        this.interval = timeinterval*2;
        this.job = scheduler.scheduleAtFixedRate(this, 30, this.interval, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            if(this.peerManager.checkIfDone()) {
                this.peerManager.stopAllPeerControllers();
                this.cancelJob();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelJob() {
        this.scheduler.shutdownNow();
    }
}
