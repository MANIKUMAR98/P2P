package src;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OptimisticUnchokeHandler implements Runnable {
    private int interval;
    private PeerManager peerManager;
    private Random rand = new Random();
    private ScheduledFuture<?> job = null;
    private ScheduledExecutorService scheduler = null;

    OptimisticUnchokeHandler(PeerManager peerManager) {
        this.peerManager = peerManager;
        this.interval = peerManager.getOptimisticUnChokeFrequency();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startJob() {
        this.job = this.scheduler.scheduleAtFixedRate(this, 6, this.interval, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            String optUnchoked = this.peerManager.getOptimisticUnChokedPeer();
            List<String> interested = new ArrayList<String>(this.peerManager.getInterestedPeerList());
            if(interested.size() >0)
                interested.remove(optUnchoked);
            int iLen = interested.size();
            if (iLen > 0) {
                String nextPeer = interested.get(rand.nextInt(iLen));
                while (this.peerManager.getUnChokedPeerList().contains(nextPeer)) {
                    interested.remove(nextPeer);
                    iLen--;
                    if(iLen > 0) {
                        nextPeer = interested.get(rand.nextInt(iLen));
                    }
                    else {
                        nextPeer = null;
                        break;
                    }
                }
                this.peerManager.setOptimisticUnChokedPeer(nextPeer);
                if(nextPeer != null) {
                    PeerController nextHandler = this.peerManager.getPeerController(nextPeer);
                    nextHandler.messageSender.issueUnChokeMessage();
                    this.peerManager.getClientLogger().storeUnchokedNeighborLog(this.peerManager.getOptimisticUnChokedPeer());
                }
                if (optUnchoked != null && !this.peerManager.getUnChokedPeerList().contains(optUnchoked)) {
                    this.peerManager.getPeerController(optUnchoked).messageSender.issueChokeMessage();
                }
            }
            else {
                String currentOpt = this.peerManager.getOptimisticUnChokedPeer();
                this.peerManager.setOptimisticUnChokedPeer(null);
                if (currentOpt != null && !this.peerManager.getUnChokedPeerList().contains(currentOpt)) {
                    PeerController nextHandler = this.peerManager.getPeerController(currentOpt);
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

    public void cancelJob() {
        this.scheduler.shutdownNow();
    }
}
