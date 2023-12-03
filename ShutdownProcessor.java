

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ShutdownProcessor implements Runnable {
    private int timeSpan;
    private PeerManager peerManager;
    private ScheduledFuture<?> scheduledFuture = null;
    private ScheduledExecutorService scheduledExecutorService = null;

    ShutdownProcessor(PeerManager peerManager) {
        this.peerManager = peerManager;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    public void initilizeJob(int timeinterval) {
        this.timeSpan = timeinterval*2;
        this.scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this, 30, this.timeSpan, TimeUnit.SECONDS);
    }

    /**
     * Executes the run logic, checking if all peers have completed downloading.
     * If all peers are done, it stops all PeerControllers and aborts the job.
     * Handles exceptions by printing the stack trace.
     */
    public void run() {
        try {
            if(this.peerManager.checkIfDone()) {
            	// Stop all PeerControllers and abort the job
                this.peerManager.stopAllPeerControllers();
                this.abortJob();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void abortJob() {
        this.scheduledExecutorService.shutdownNow();
    }
}
