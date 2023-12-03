

import java.io.*;
import java.util.*;

public class CommonConfiguration {
    public int numberOfPreferredNeighbors;
    public int unchokingInterval;
    public int optimisticUnchokingInterval;
    public String fileName;
    public int fileSize;
    public int pieceSize;

    public void InitilizeCommonConfiguration() {
    	FileReader fileReader = null;
        Scanner scanner = null;
        try {
            fileReader = new FileReader("Common.cfg");
            scanner = new Scanner(fileReader);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] temp = line.split(" ");
                String key = temp[0];
                String value = temp[1];
                switch (key) {
					case Constants.NUMBER_OF_PREFERRED_NEIGHBORS:
						this.numberOfPreferredNeighbors = Integer.parseInt(value);
						break;
					case Constants.UNCHOKING_INTERVAL:
						this.unchokingInterval = Integer.parseInt(value);
						break;
					case Constants.OPTIMISTIC_UNCHOKING_INTERVAL:
						this.optimisticUnchokingInterval = Integer.parseInt(value);
						break;
					case Constants.FILE_NAME:
						this.fileName = value;
						break;
					case Constants.FILE_SIZE:
						this.fileSize = Integer.parseInt(value);
						break;
					case Constants.PIECE_SIZE:
						this.pieceSize = Integer.parseInt(value);
						break;
				default:
					throw new IllegalArgumentException("Unexpected value: " + key);
				}
            }
        } 
        catch (Exception ex) {
            System.err.println("Exception occurred while loading the Common.cfg file "+ex);
        } finally {
        		try {
        			if(fileReader != null) {
        				fileReader.close();
        			}
					if(scanner != null) {
						scanner.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
    }
}
