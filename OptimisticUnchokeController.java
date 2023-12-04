

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OptimisticUnchokeController implements Runnable {
    private int timeSpan;
    private PeerManager peerManager;
    private Random rand = new Random();
    private ScheduledFuture<?> scheduledFuture = null;
    private ScheduledExecutorService scheduledExecutorService = null;

    OptimisticUnchokeController(PeerManager peerManager) {
        this.peerManager = peerManager;
        this.timeSpan = peerManager.getOptimisticUnChokeFrequency();
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    /**
     * Initializes the scheduled job by scheduling it at a fixed rate.
     * Uses the scheduledExecutorService to periodically execute the 'run' method
     * of the current instance at the specified time intervals. The scheduledFuture
     * is assigned the reference to the scheduled task, allowing for potential
     * cancellation or querying of the task status.
     */
    public void initilizeTheJob() {
        this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(this, 6, this.timeSpan, TimeUnit.SECONDS);
    }

    /**
     * Executes the optimistic unchoking to select an optimistically unchoked peer.
     * Selects a random peer from the interested peer list, excluding the current optimistically
     * unchoked peer if present. Sends unchoke messages to the newly selected peer and logs the update.
     * Additionally, sends choke messages to the previous optimistically unchoked peer if necessary.
     * If no interested peers are available, cancels optimistic unchoking and checks for completion.
     */
    public void run() {
        try {
        	// Retrieve current optimistic unchoked peer and interested peer list
            String optimisticUnchokedPeer = this.peerManager.getOptimisticUnChokedPeer();
            List<String> interestedPeerList = new ArrayList<String>(this.peerManager.getInterestedPeerList());
            // Exclude the current optimistically unchoked peer
            if(interestedPeerList.size() > 0)
            	interestedPeerList.remove(optimisticUnchokedPeer);
            int size = interestedPeerList.size();
            if (size > 0) {
                String nextInterestedPeer = interestedPeerList.get(rand.nextInt(size));
                while (this.peerManager.getUnChokedPeerList().contains(nextInterestedPeer)) {
                	interestedPeerList.remove(nextInterestedPeer);
                	size--;
                    if(size > 0) {
                    	nextInterestedPeer = interestedPeerList.get(rand.nextInt(size));
                    }
                    else {
                    	nextInterestedPeer = null;
                        break;
                    }
                }
                // Set the new optimistically unchoked peer and send an unchoke message
                this.peerManager.setOptimisticUnChokedPeer(nextInterestedPeer);
                if(nextInterestedPeer != null) {
                    PeerController nextHandler = this.peerManager.getPeerController(nextInterestedPeer);
                    nextHandler.messageSender.issueUnChokeMessage();
                    this.peerManager.getClientLogger().updateOptimisticallyUnchokedNeighbor(this.peerManager.getOptimisticUnChokedPeer());
                }
                if (optimisticUnchokedPeer != null && !this.peerManager.getUnChokedPeerList().contains(optimisticUnchokedPeer)) {
                    this.peerManager.getPeerController(optimisticUnchokedPeer).messageSender.issueChokeMessage();
                }
            }
            else {
                String optimisticUnChokedPeer = this.peerManager.getOptimisticUnChokedPeer();
                this.peerManager.setOptimisticUnChokedPeer(null);
                // If there was a previous optimistically unchoked peer, send a choke message
                if (optimisticUnChokedPeer != null && !this.peerManager.getUnChokedPeerList().contains(optimisticUnChokedPeer)) {
                    PeerController nextHandler = this.peerManager.getPeerController(optimisticUnChokedPeer);
                    nextHandler.messageSender.issueChokeMessage();
                }
             // If all peers have completed downloading, cancel chokes
                if(this.peerManager.areAllPeersDone()) {
                    this.peerManager.cancelChokes();
                }
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
