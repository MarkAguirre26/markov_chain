package com.baccarat.markovchain.module.services.impl;

import org.springframework.stereotype.Service;

@Service
public class PatternRecognizer {
    /**
     * Finds the longest repeating pattern at the end of the given sequence.
     *
     * @param sequence the input string sequence
     * @return the longest repeating pattern, or an empty string if no pattern is found
     */
    public String findPattern(String sequence) {
        int len = sequence.length();

        // Return an empty string for empty input
        if (len == 0) return "";

        // Iterate through possible pattern lengths
        for (int patternLength = 1; patternLength <= len / 2; patternLength++) {
            String candidatePattern = sequence.substring(len - patternLength);

            // Check if the sequence ends with the candidate pattern repeated twice
            if (sequence.endsWith(candidatePattern + candidatePattern)) {
                return candidatePattern; // Return the first matching pattern found
            }
        }
        return ""; // Return an empty string if no pattern is found
    }

}