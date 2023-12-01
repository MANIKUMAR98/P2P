package src;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toMap;

public class ChokeController implements Runnable {
    private int interval;
    private int preferredNeighboursCount;
    private PeerManager peerAdmin;
    private Random rand = new Random();
    private ScheduledFuture<?> job = null;
    private ScheduledExecutorService scheduler = null;

    ChokeController(PeerManager peerManager) {
        this.peerAdmin = peerManager;
        this.interval = peerManager.getUnchockingInterval();
        this.preferredNeighboursCount = peerManager.getNoOfPreferredNeighbors();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startJob() {
        this.job = this.scheduler.scheduleAtFixedRate(this, 6, this.interval, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            Set<String> unchokedPeerList = new HashSet<>(this.peerAdmin.getUnchokedList());
            Set<String> newlist = new HashSet<>();
            List<String> interested = new ArrayList<String>(this.peerAdmin.getInterestedList());
            if (interested.size() > 0) {
            	int iter = 0;
            	if(this.preferredNeighboursCount < interested.size()) {
            		iter = this.preferredNeighboursCount;
            	} else {
            		iter = interested.size();
            	}
                if (this.peerAdmin.getCompletedPieceCount() == this.peerAdmin.getPieceCount()) {
                    for (int i = 0; i < iter; i++) {
                        String nextPeer = interested.get(this.rand.nextInt(interested.size()));
                        PeerController nextHandler = this.peerAdmin.getPeerHandler(nextPeer);
//                      /for not selecting not se;ecting same peer which is in newlist
                        while (newlist.contains(nextPeer)) {
                            nextPeer = interested.get(this.rand.nextInt(interested.size()));
                            nextHandler = this.peerAdmin.getPeerHandler(nextPeer);
                        }
                        if (!unchokedPeerList.contains(nextPeer)) {
                            if (this.peerAdmin.getOptimisticUnchokedPeer() == null
                                    || this.peerAdmin.getOptimisticUnchokedPeer().compareTo(nextPeer) != 0) {
                                this.peerAdmin.getUnchokedList().add(nextPeer);
                                nextHandler.messageSender.issueUnChokeMessage();

                            }
                        }
                        else {
                        	unchokedPeerList.remove(nextPeer);
                        }
                        newlist.add(nextPeer);
                        nextHandler.resetDownloadRate();
                    }
                }
                else {
                    Map<String, Integer> downloads = new HashMap<>(this.peerAdmin.getDownloadRates());
                    Map<String, Integer> rates = downloads.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
                    Iterator<Map.Entry<String, Integer>> iterator = rates.entrySet().iterator();
                    int counter = 0;
                    while (counter < iter && iterator.hasNext()) {
                        Map.Entry<String, Integer> ent = iterator.next();
                        if (interested.contains(ent.getKey())) {
                            PeerController nextHandler = this.peerAdmin.getPeerHandler(ent.getKey());
                            if (!unchokedPeerList.contains(ent.getKey())) {
                                String optUnchoke = this.peerAdmin.getOptimisticUnchokedPeer();
                                if (optUnchoke == null || optUnchoke.compareTo(ent.getKey()) != 0) {
                                    this.peerAdmin.getUnchokedList().add(ent.getKey());
                                    nextHandler.messageSender.issueUnChokeMessage();

                                }
                            }
                            else {
                            	unchokedPeerList.remove(ent.getKey());
                            }
                            newlist.add(ent.getKey());
                            nextHandler.resetDownloadRate();
                            counter++;
                        }
                    }
                }
                
                this.issueChokeMessage(unchokedPeerList);
                this.peerAdmin.updateUnchokedList(newlist);
                if(newlist.size() > 0){
                    this.peerAdmin.getClientLogger().updatePreferredNeighbors(new ArrayList<>(newlist));
                }
            }
            else {
                this.peerAdmin.resetUnchokedList();
                this.issueChokeMessage(unchokedPeerList);
                if(this.peerAdmin.checkIfAllPeersAreDone()) {
                    this.peerAdmin.cancelChokes();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void issueChokeMessage(Set<String> unchokedPeerList) {
    	unchokedPeerList.forEach(peer -> {
        	PeerController nextHandler = this.peerAdmin.getPeerHandler(peer);
            nextHandler.messageSender.issueChokeMessage();
        });
    }

    public void cancelJob() {
        this.scheduler.shutdownNow();
    }
}
