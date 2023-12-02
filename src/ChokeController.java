package src;

import java.util.*;
import java.util.concurrent.*;


public class ChokeController implements Runnable {
    private int timeSpan;
    private int neighborCount;
    private PeerManager peerManager;
    private Random rand = new Random();
    private ScheduledFuture<?> scheduledFuture = null;
    private ScheduledExecutorService scheduledExecutorService = null;

    ChokeController(PeerManager peerManager) {
        this.peerManager = peerManager;
        this.timeSpan = peerManager.getUnChokeFrequency();
        this.neighborCount = peerManager.getPreferredNeighborNumber();
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    public void initilizeTheJob() {
        this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(this, 6, this.timeSpan, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            Set<String> unchokedPeerList = new HashSet<>(this.peerManager.getUnChokedPeerList());
            Set<String> peersList = new HashSet<>();
            List<String> interestedPeerList = new ArrayList<String>(this.peerManager.getInterestedPeerList());
            if (interestedPeerList.size() > 0) {
                int j = 0;
                if(this.neighborCount < interestedPeerList.size()) {
                    j = this.neighborCount;
                } else {
                    j = interestedPeerList.size();
                }
                if (this.peerManager.getAvailableChunkCount() == this.peerManager.getChunkCount()) {
                    for (int i = 0; i < j; i++) {
                        String interestedPeer = interestedPeerList.get(this.rand.nextInt(interestedPeerList.size()));
                        PeerController peerController = this.peerManager.getPeerController(interestedPeer);
                        while (peersList.contains(interestedPeer)) {
                        	interestedPeer = interestedPeerList.get(this.rand.nextInt(interestedPeerList.size()));
                        	peerController = this.peerManager.getPeerController(interestedPeer);
                        }
                        if (!unchokedPeerList.contains(interestedPeer)) {
                            if (this.peerManager.getOptimisticUnChokedPeer() == null || this.peerManager.getOptimisticUnChokedPeer().compareTo(interestedPeer) != 0) {
                                this.peerManager.getUnChokedPeerList().add(interestedPeer);
                                peerController.messageSender.issueUnChokeMessage();
                            }
                        }
                        else {
                            unchokedPeerList.remove(interestedPeer);
                        }
                        peersList.add(interestedPeer);
                        peerController.resetDownloadRate();
                    }
                }
                else {
                    Map<String, Integer> chunkDownloadRates = new HashMap<>(this.peerManager.getChunkDownloadRates());
                    List<Map.Entry<String, Integer>> entryList = new ArrayList<>(chunkDownloadRates.entrySet());
                    Collections.sort(entryList, (a, b) -> b.getValue() - a.getValue());
                    int counter = 0, i = 0;
                    while (counter < j && i < entryList.size()) {
                        Map.Entry<String, Integer> entry = entryList.get(i); i++;
                        if (interestedPeerList.contains(entry.getKey())) {
                            PeerController nextPeerController = this.peerManager.getPeerController(entry.getKey());
                            if (!unchokedPeerList.contains(entry.getKey())) {
                                String optimisticUnChokedPeer = this.peerManager.getOptimisticUnChokedPeer();
                                if (optimisticUnChokedPeer == null || optimisticUnChokedPeer.compareTo(entry.getKey()) != 0) {
                                    this.peerManager.getUnChokedPeerList().add(entry.getKey());
                                    nextPeerController.messageSender.issueUnChokeMessage();
                                }
                            }
                            else {
                                unchokedPeerList.remove(entry.getKey());
                            }
                            peersList.add(entry.getKey());
                            nextPeerController.resetDownloadRate();
                            counter++;
                        }
                    }
                }

                this.issueChokeMessage(unchokedPeerList);
                this.peerManager.updateUnChokedPeerList(peersList);
                if(peersList.size() > 0){
                    this.peerManager.getClientLogger().updatePreferredNeighbors(new ArrayList<>(peersList));
                }
            }
            else {
                this.peerManager.resetUnChokedPeerList();
                this.issueChokeMessage(unchokedPeerList);
                if(this.peerManager.areAllPeersDone()) {
                    this.peerManager.cancelChokes();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueChokeMessage(Set<String> unchokedPeerList) {
        unchokedPeerList.forEach(peer -> {
            PeerController nextHandler = this.peerManager.getPeerController(peer);
            nextHandler.messageSender.issueChokeMessage();
        });
    }

    public void abortJob() {
        this.scheduledExecutorService.shutdownNow();
    }
}
