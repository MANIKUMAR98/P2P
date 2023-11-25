package src;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utility {
	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a");
	
	public synchronized String getCurrentTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        return currentTime.format(formatter);
	}

}
