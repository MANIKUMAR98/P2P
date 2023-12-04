

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Utility {
	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a");
	
	/**
	 * Retrieves the current time as a formatted string.
	 * Uses the LocalDateTime class to get the current time and formats it using
	 * the formatter. Returns the formatted time as a string.
	 *
	 * @return The current time as a formatted string.
	 */
	public synchronized String getCurrentTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        return currentTime.format(formatter);
	}
	
	/**
	 * Converts a list of strings to a comma-separated string.
	 * Concatenates the elements of the list with commas and returns the resulting string.
	 *
	 * @param list The list of strings to be converted.
	 * @return The comma-separated string representation of the list.
	 */

	public String getStringFromList(List<String> list) {
		 String string = "";
	        for (String neigh : list) {
	        	string += neigh + ",";
	        }
	     return string.substring(0, string.length() - 1);
	}

}
