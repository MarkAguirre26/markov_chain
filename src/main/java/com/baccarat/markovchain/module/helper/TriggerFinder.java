package com.baccarat.markovchain.module.helper;

public class TriggerFinder {

    public static boolean isEntryTriggerExists(String handResult,String entry) {
        return handResult.contains(entry);
    }


    public static boolean isStopTriggerExists(String handResult,String entry,String stopKey) {
        int wIndex = handResult.indexOf(entry);
        return wIndex != -1 && handResult.indexOf(stopKey, wIndex + 1) != -1;
    }


}