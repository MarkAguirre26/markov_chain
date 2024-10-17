package com.baccarat.markovchain.module.helper;

import org.hibernate.event.spi.SaveOrUpdateEvent;

public class ShoePatternAnalyzer {

    public static boolean isShoePatternTrendChoppy(String outcomes) {

        if (outcomes == null || outcomes.isEmpty()) {
            return true;  // Consider null or empty as choppy
        }

        // Ensure the string has at least two characters
        if (outcomes.length() < 2) {
            System.out.println("The string is too short.");
        } else {

            String lastCharacter = String.valueOf(outcomes.charAt(outcomes.length() - 1));
            String secondToLastCharacter = String.valueOf(outcomes.charAt(outcomes.length() - 2));

            System.out.println("Last Character: " + lastCharacter);
            System.out.println("Second to Last Character: " + secondToLastCharacter);
            return !(lastCharacter.equals(secondToLastCharacter) && lastCharacter.equals("W"));


        }
        return false;

    }
}
