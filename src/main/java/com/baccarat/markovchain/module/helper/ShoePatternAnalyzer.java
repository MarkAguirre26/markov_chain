package com.baccarat.markovchain.module.helper;

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

            if (!lastCharacter.equals(secondToLastCharacter)) {
             return true;
            }


        }
        return false;

    }
}
