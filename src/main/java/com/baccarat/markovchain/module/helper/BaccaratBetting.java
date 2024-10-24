package com.baccarat.markovchain.module.helper;

import com.baccarat.markovchain.module.data.response.GameResultResponse;

public class BaccaratBetting {

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

    public static int kissOneTwoThree(GameResultResponse gameResultResponse) {
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
                if (skipOutcome == 'N') {
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


    public static int oneThreeTwoSix(GameResultResponse gameResultResponse) {

        String handResult = gameResultResponse.getHandResult();
        String skipSequence = gameResultResponse.getSkipState();

        // Ensure both sequences have the same length to avoid errors
        if (handResult == null) {
            return 1;
        }

        // 1-3-2-6 progression bets
        int initialBet = 1;
        int[] progression = {1, 3, 2, 3};
        int currentStage = 0;
        int currentBetUnit = initialBet;

        // Iterate through the results and skip states
        for (int i = 0; i < handResult.length(); i++) {
            char outcome = handResult.charAt(i);
            char skipOutcome = skipSequence.charAt(i);

            // Only proceed if not skipped
            if (skipOutcome == 'N') {
                if (outcome == 'W') {
                    // Win: Return the current progression value and move to next stage


                    // Move to the next stage in the progression
                    currentStage++;
                    if (currentStage == progression.length) {
                        // Reset progression if the 6-unit stage is reached
                        currentStage = 0;
//                        currentBetUnit = initialBet;
                    }
                    currentBetUnit = progression[currentStage] * initialBet;

                } else if (outcome == 'L') {

                    currentBetUnit = initialBet;
                    // Reset the progression after a loss
                    currentStage = 0;
                }
            }
        }

        return currentBetUnit;  // Return the last progression value used
    }

}
