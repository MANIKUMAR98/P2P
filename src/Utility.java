package src;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Utility {
	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a");
	
	public synchronized String getCurrentTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        return currentTime.format(formatter);
	}
	
	public String getStringFromList(List<String> list) {
		 String string = "";
	        for (String neigh : list) {
	        	string += neigh + ",";
	        }
	     return string.substring(0, string.length() - 1);
	}

}
