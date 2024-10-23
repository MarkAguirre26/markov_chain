package com.baccarat.markovchain.module.helper;

import com.baccarat.markovchain.module.data.response.GameResultResponse;

public class BaccaratKISS123 {

//    public static void main(String[] args) {
//        // Define the sequence of wins (W) and losses (L)
//        String sequence = "WWLLWLWLWLWWLLWWWWWW";
//
//        // Get the bet units for the last round
//        int lastBetUnits = calculateLastBetUnit(sequence);
//
//        // Output the result (bet units for the last round)
//        System.out.println("Bet " + lastBetUnits + " units for the last round");
//    }

    public static int calculateLastBetUnit(GameResultResponse gameResultResponse) {
        String handResult = gameResultResponse.getHandResult();
        String skipSequence = gameResultResponse.getSkipState();

        if (handResult == null) {
            return 1;
        }
        int currentBetUnit = 1;  // Start at Unit 1
        int currentStage = 1;    // Stage progression: 1 unit, 2 units, 3 units

        // Loop through the sequence of wins ('W') and losses ('L')
        for (int i = 0; i < handResult.length(); i++) {
            char outcome = handResult.charAt(i);
            char skipOutcome = skipSequence.charAt(i);


            if (outcome == 'W') {
                // Win scenario: always reset the bet to Unit 1
                if(skipOutcome == 'N'){
                    currentStage = 1;
                    currentBetUnit = 1;
                }

            } else if (outcome == 'L') {
                // Loss scenario: progress to the next stage
                if (skipOutcome == 'N') {
                    if (currentStage == 1) {
                        currentStage = 2;
                        currentBetUnit = 2;  // Bet 2 units after the first loss
                    } else if (currentStage == 2) {
                        currentStage = 3;
                        currentBetUnit = 3;  // Bet 3 units after the second loss
                    } else {
                        // After reaching Unit 3, reset back to Unit 1
                        currentStage = 1;
                        currentBetUnit = 1;
                    }
                }

            }
        }

        return currentBetUnit;  // Return the bet units for the last round
    }
}
