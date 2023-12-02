package src;

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

    public void initilizeTheJob() {
        this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(this, 6, this.timeSpan, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            String optimisticUnchokedPeer = this.peerManager.getOptimisticUnChokedPeer();
            List<String> interestedPeerList = new ArrayList<String>(this.peerManager.getInterestedPeerList());
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
                this.peerManager.setOptimisticUnChokedPeer(nextInterestedPeer);
                if(nextInterestedPeer != null) {
                    PeerController nextHandler = this.peerManager.getPeerController(nextInterestedPeer);
                    nextHandler.messageSender.issueUnChokeMessage();
                    this.peerManager.getClientLogger().storeUnchokedNeighborLog(this.peerManager.getOptimisticUnChokedPeer());
                }
                if (optimisticUnchokedPeer != null && !this.peerManager.getUnChokedPeerList().contains(optimisticUnchokedPeer)) {
                    this.peerManager.getPeerController(optimisticUnchokedPeer).messageSender.issueChokeMessage();
                }
            }
            else {
                String optimisticUnChokedPeer = this.peerManager.getOptimisticUnChokedPeer();
                this.peerManager.setOptimisticUnChokedPeer(null);
                if (optimisticUnChokedPeer != null && !this.peerManager.getUnChokedPeerList().contains(optimisticUnChokedPeer)) {
                    PeerController nextHandler = this.peerManager.getPeerController(optimisticUnChokedPeer);
                    nextHandler.messageSender.issueChokeMessage();
                }
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
