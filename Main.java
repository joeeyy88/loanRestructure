package com.temenos.tdf.loanRestructure;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        // Get the current timestamp as an Instant object
        Instant timestamp = Instant.now();
        
        // Format the timestamp into the required format (for example, "yyyy-MM-dd HH:mm:ss")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Riyadh"));  // KSA Time Zone
        
        String formattedTimestamp = formatter.format(timestamp);
        
        System.out.println("Current KSA Timestamp: " + formattedTimestamp);
    }
}
