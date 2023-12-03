

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

    /**
     * Initializes and starts the job by scheduling it at a fixed rate using the
     * configured time span. This method uses the scheduledExecutorService to
     * periodically execute the 'run' method of the current instance at the specified
     * time intervals. The scheduledFuture is assigned the reference to the
     * scheduled task, allowing for potential cancellation or querying of the
     * task status.
     */
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
                // Check if all available chunks have been downloaded by the peer
                if (this.peerManager.getAvailableChunkCount() == this.peerManager.getChunkCount()) {
                	 // Iterate through a specified number of iterations (j)
                    for (int i = 0; i < j; i++) {
                    	// Randomly select a peer from the list of interested peers
                        String interestedPeer = interestedPeerList.get(this.rand.nextInt(interestedPeerList.size()));
                        PeerController peerController = this.peerManager.getPeerController(interestedPeer);
                        // Ensure the selected peer is not already in the peersList
                        while (peersList.contains(interestedPeer)) {
                        	interestedPeer = interestedPeerList.get(this.rand.nextInt(interestedPeerList.size()));
                        	peerController = this.peerManager.getPeerController(interestedPeer);
                        }
                        // Check if the interested peer is not in the unchokedPeerList
                        if (!unchokedPeerList.contains(interestedPeer)) {
                        	// Check if there is no optimistic unchoked peer or the selected peer is not the optimistic unchoked peer
                            if (this.peerManager.getOptimisticUnChokedPeer() == null || this.peerManager.getOptimisticUnChokedPeer().compareTo(interestedPeer) != 0) {
                            	// Add the interested peer to the unchokedPeerList and send an Unchoke message
                            	this.peerManager.getUnChokedPeerList().add(interestedPeer);
                                peerController.messageSender.issueUnChokeMessage();
                            }
                        }
                        else {
                        	// If the interested peer is already in the unchokedPeerList, remove it
                            unchokedPeerList.remove(interestedPeer);
                        }
                        // Add the interested peer to the peersList and reset its download rate
                        peersList.add(interestedPeer);
                        peerController.resetDownloadRate();
                    }
                } else {
                	/**
                	 * Selects and manages unchoked peers based on their download rates.
                	 * It creates a sorted list of peers by their download rates in descending order.
                	 * The method iterates through the sorted list, selecting peers that are in the
                	 * interestedPeerList, not already in the unchokedPeerList, and not the optimistic
                	 * unchoked peer. Selected peers are added to the unchoked list, and Unchoke
                	 * messages are issued to them. If a peer is already in the unchokedPeerList,
                	 * it is removed. The method also tracks peers in the peersList and resets
                	 * their download rates.
                	 * */
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

    /**
     * Aborts the job by shutting down the scheduled executor service.
     * This method forcefully terminates the scheduled executor service,
     * stopping all scheduled tasks and preventing any new tasks from being
     * accepted. It is used to halt ongoing processes when an abort condition
     * is encountered.
     */
    public void abortJob() {
        this.scheduledExecutorService.shutdownNow();
    }
}
