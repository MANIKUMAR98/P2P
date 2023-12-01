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
        this.interval = peerManager.getOptimisticUnchockingInterval();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startJob() {
        this.job = this.scheduler.scheduleAtFixedRate(this, 6, this.interval, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            String optUnchoked = this.peerManager.getOptimisticUnchokedPeer();
            List<String> interested = new ArrayList<String>(this.peerManager.getInterestedList());
            if(interested.size() >0)
                interested.remove(optUnchoked);
            int iLen = interested.size();
            if (iLen > 0) {
                String nextPeer = interested.get(rand.nextInt(iLen));
                while (this.peerManager.getUnchokedList().contains(nextPeer)) {
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
                this.peerManager.setOptimisticUnchokdPeer(nextPeer);
                if(nextPeer != null) {
                    PeerController nextHandler = this.peerManager.getPeerHandler(nextPeer);
                    nextHandler.messageSender.issueUnChokeMessage();
                    this.peerManager.getClientLogger().storeUnchokedNeighborLog(this.peerManager.getOptimisticUnchokedPeer());
                }
                if (optUnchoked != null && !this.peerManager.getUnchokedList().contains(optUnchoked)) {
                    this.peerManager.getPeerHandler(optUnchoked).messageSender.issueChokeMessage();
                }
            }
            else {
                String currentOpt = this.peerManager.getOptimisticUnchokedPeer();
                this.peerManager.setOptimisticUnchokdPeer(null);
                if (currentOpt != null && !this.peerManager.getUnchokedList().contains(currentOpt)) {
                    PeerController nextHandler = this.peerManager.getPeerHandler(currentOpt);
                    nextHandler.messageSender.issueChokeMessage();
                }
                if(this.peerManager.checkIfAllPeersAreDone()) {
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
