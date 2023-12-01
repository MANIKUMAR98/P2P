package src;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OptimisticUnchokeHandler implements Runnable {
    private int interval;
    private PeerAdmin peerAdmin;
    private Random rand = new Random();
    private ScheduledFuture<?> job = null;
    private ScheduledExecutorService scheduler = null;

    OptimisticUnchokeHandler(PeerAdmin padmin) {
        this.peerAdmin = padmin;
        this.interval = padmin.getOptimisticUnchockingInterval();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startJob() {
        this.job = this.scheduler.scheduleAtFixedRate(this, 6, this.interval, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            String optUnchoked = this.peerAdmin.getOptimisticUnchokedPeer();
            List<String> interested = new ArrayList<String>(this.peerAdmin.getInterestedList());
            if(interested.size() >0)
                interested.remove(optUnchoked);
            int iLen = interested.size();
            if (iLen > 0) {
                String nextPeer = interested.get(rand.nextInt(iLen));
                while (this.peerAdmin.getUnchokedList().contains(nextPeer)) {
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
                this.peerAdmin.setOptimisticUnchokdPeer(nextPeer);
                if(nextPeer != null) {
                    PeerController nextHandler = this.peerAdmin.getPeerHandler(nextPeer);
                    nextHandler.messageSender.issueUnChokeMessage();
                    this.peerAdmin.getClientLogger().storeUnchokedNeighborLog(this.peerAdmin.getOptimisticUnchokedPeer());
                }
                if (optUnchoked != null && !this.peerAdmin.getUnchokedList().contains(optUnchoked)) {
                    this.peerAdmin.getPeerHandler(optUnchoked).messageSender.issueChokeMessage();
                }
            }
            else {
                String currentOpt = this.peerAdmin.getOptimisticUnchokedPeer();
                this.peerAdmin.setOptimisticUnchokdPeer(null);
                if (currentOpt != null && !this.peerAdmin.getUnchokedList().contains(currentOpt)) {
                    PeerController nextHandler = this.peerAdmin.getPeerHandler(currentOpt);
                    nextHandler.messageSender.issueChokeMessage();
                }
                if(this.peerAdmin.checkIfAllPeersAreDone()) {
                    this.peerAdmin.cancelChokes();
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
