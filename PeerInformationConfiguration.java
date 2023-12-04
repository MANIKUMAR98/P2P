

import java.util.*;
import java.io.*;

public class PeerInformationConfiguration {
	
	private Map<String, PeerInformation> remotePeerInformationMap;
	private List<String> peerInformation;

	public PeerInformationConfiguration(){
		this.remotePeerInformationMap = new HashMap<>();
		this.peerInformation = new ArrayList<>();
	}

	/**
	 * Initializes peer information from the "PeerInfo.cfg" file.
	 * Reads data from the file, splits it into tokens, and creates PeerInformation
	 * objects for each peer. Populates the remotePeerInformationMap and peerInformation list.
	 * Handles exceptions by printing an error message.
	 */
	public void initilizePeerInformationFile() {
		String data;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while((data = br.readLine()) != null) {
				String[] tokens = data.split("\\s+");
				this.remotePeerInformationMap.put(tokens[0], new PeerInformation(tokens[0], tokens[1], tokens[2], tokens[3]));
				this.peerInformation.add(tokens[0]);
			}
			br.close();
		}
		catch (Exception ex) {
			System.err.println("Exception occurred while loading PeerInfo.cfg file "+ex);
		}
	}

	public PeerInformation getPeerInfoConfiguration(String peerID){
		return this.remotePeerInformationMap.get(peerID);
	}

	public Map<String, PeerInformation> getRemotePeerInformationMap(){
		return this.remotePeerInformationMap;
	}

	public List<String> getPeerInformation(){
		return this.peerInformation;
	}
}
