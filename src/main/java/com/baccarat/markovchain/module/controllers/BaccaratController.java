package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.model.Journal;
import com.baccarat.markovchain.module.model.Pair;
import com.baccarat.markovchain.module.model.Session;
import com.baccarat.markovchain.module.services.SessionService;
import com.baccarat.markovchain.module.services.impl.JournalServiceImpl;
import com.baccarat.markovchain.module.services.impl.MarkovChain;
import com.baccarat.markovchain.module.services.impl.PatternRecognizer;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/baccarat")
public class BaccaratController {

    private static final Logger logger = LoggerFactory.getLogger(BaccaratController.class);
    private static final double STOP_PROFIT_PERCENTAGE = 0.40;
    private static final double STOP_LOSS_PERCENTAGE = 0.10;
    private static final double VIRTUAL_WIN_PROBABILITY = 0.5;
    private static final double BASE_BET = 10.0;
    private static final double INITIAL_PLAYING_FUND = 1000.0;
    private static final int MAX_DAILY_JOURNAL_LIMIT = 10;
    private final MarkovChain markovChain;
    private final PatternRecognizer patternRecognizer;
    private final JournalServiceImpl journalService;
    private final SessionService sessionService;
    private String sequence = ""; // Empty starting sequence
    private String WAIT = "Wait..";
    private int handCount = 0, totalWins = 0, totalLosses = 0, handLimit = 60;
    private double profit = 0, playingFund = INITIAL_PLAYING_FUND;
    private boolean waitingForVirtualWin = false;

    @Autowired
    public BaccaratController(MarkovChain markovChain, PatternRecognizer patternRecognizer, JournalServiceImpl journalService, SessionService sessionService) {
        this.markovChain = markovChain;
        this.patternRecognizer = patternRecognizer;
        this.journalService = journalService;
        this.sessionService = sessionService;
    }

    @PostMapping("/play")
    public GameResponse play(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                             @RequestParam String userInput,
                             @RequestParam String recommendedBet,
                             @RequestParam double baseBetAmount) {

        // Log input values
        logger.info("Authorization header: {}", authorizationHeader);
        logger.info("Received user input: {}", userInput);
        logger.info("Received recommendedBet input: {}", recommendedBet);
        logger.info("Received baseBetAmount input: {}", baseBetAmount);

        // Validate user input
        if (!isValidInput(userInput)) {
            return createErrorResponse();
        }

        String id = "957baf71-80c4-11ef-a303-f02f748a05bf";

        // Fetch session using authorization header or default session value
        Optional<Session> session = getSession(id);

        if (session.isEmpty()) {
            return createGameLimitResponse(sequence);
        }

        // Check if the user has reached the daily journal limit
        if (hasReachedDailyJournalLimit(session.get().getUserUuid())) {
            return createGameLimitResponse(sequence);
        }

        // Handle hand limits, stop profit/loss conditions
        if (hasReachedHandLimit() || hasReachedStopConditions()) {
            journalService.saveJournal(new Journal(0, "", 0, getGameStatus().getHandCount(),
                    getGameStatus().getProfit(), LocalDateTime.now(), LocalDate.now()));
            return new GameResponse("Game condition reached! Restart the game.", sequence, getGameStatus(), 0, WAIT);
        }

        // Process virtual win if applicable
        if (waitingForVirtualWin) {
            return processVirtualWin();
        }

        updateSequence(userInput);


        // Generate predictions using Markov chain and pattern recognition
        markovChain.train(sequence);
        Pair<Character, Double> markovPrediction = markovChain.predictNext(sequence.charAt(sequence.length() - 1));
        String patternPrediction = patternRecognizer.findPattern(sequence);

        logger.info("Markov Prediction: {}, Pattern Prediction: {}", markovPrediction, patternPrediction);

        // Combine predictions and handle the bet
        Pair<Character, Double> combinedPrediction = combinePredictions(markovPrediction, patternPrediction);
        return handleBet(userInput, combinedPrediction, recommendedBet, baseBetAmount);
    }

    private Optional<Session> getSession(String authorizationHeader) {
        String sessionValue = (authorizationHeader != null) ? authorizationHeader : ""; // Fallback to default session
        int isExpired = 0; // 0 -> not expired
        return sessionService.getSessionByValueAndExpired(sessionValue, isExpired);
    }

    private boolean hasReachedDailyJournalLimit(String userUuid) {
        List<Journal> journals = journalService.getJournalsByUserUuidAndDateCreated(userUuid, LocalDate.now());
        return journals.size() >= MAX_DAILY_JOURNAL_LIMIT;
    }

    private boolean hasReachedHandLimit() {
        return handCount++ == handLimit;
    }

    private boolean hasReachedStopConditions() {
        return hasReachedStopProfit() || hasReachedStopLoss();
    }

    private GameResponse createErrorResponse() {
        logger.warn("Invalid input! Please enter 'p' for Player or 'b' for Banker.");
        GameStatus gameStatus = new GameStatus(0, 0, 0, 0, 0);
        return new GameResponse("Invalid input! Please enter 'p' for Player or 'b' for Banker.", sequence, gameStatus, 0, WAIT);
    }


    private GameResponse createGameLimitResponse(String sequence) {
        logger.warn("Daily game limit hit! Please play again tomorrow");
        GameStatus gameStatus = new GameStatus(0, 0, 0, 0, 0);
        return new GameResponse("Daily game limit hit! Please play again tomorrow", sequence, gameStatus, 0, WAIT);
    }

    private boolean isValidInput(String input) {
        if (!input.equals("p") && !input.equals("b")) {
            logger.warn("Invalid input received: {}. Must be 'p' or 'b'.", input);
            return false;
        }
        return true;
    }

    private void updateSequence(String userInput) {
        sequence += userInput;
        logger.info("Updated sequence: {}", sequence);
    }

    private boolean hasReachedStopProfit() {
        if (profit >= STOP_PROFIT_PERCENTAGE * INITIAL_PLAYING_FUND) {
            logger.info("Target profit hit: {}!", profit);
            return true;
        }
        return false;
    }

    private boolean hasReachedStopLoss() {
        if (profit <= -STOP_LOSS_PERCENTAGE * INITIAL_PLAYING_FUND) {
            logger.info("Stop loss hit: {}!", profit);
            return true;
        }
        return false;
    }

    private GameResponse processVirtualWin() {
        waitingForVirtualWin = false;
        boolean isVirtualWin = Math.random() < VIRTUAL_WIN_PROBABILITY;

        if (isVirtualWin) {
            updateProfitAndFund(BASE_BET, true);
            logger.info("Virtual win! New profit: {}, New playing fund: {}", profit, playingFund);
            return new GameResponse("You won with virtual win!", sequence, getGameStatus(), 0, WAIT);
        } else {
            updateProfitAndFund(BASE_BET, false);
            logger.info("Virtual win failed. New profit: {}, New playing fund: {}", profit, playingFund);
            return new GameResponse("Virtual win failed.", sequence, getGameStatus(), 0, WAIT);
        }
    }

    private GameResponse handleBet(String userInput, Pair<Character, Double> combinedPrediction, String predictedBet, double betAmount) {

        if (combinedPrediction.first == null || combinedPrediction.second < 0.6) {
            logger.info("Prediction confidence too low. No bet suggested.");

            return new GameResponse("Prediction confidence too low, no bet suggested.", sequence, getGameStatus(), 0, WAIT);
        }

        double betSize = betAmount * combinedPrediction.second * 5;

        if (playingFund < betSize) {
            logger.warn("Not enough funds to place bet. Current Playing Fund: ${}", playingFund);

            return new GameResponse("Not enough funds to place bet. Current Playing Fund: $" + String.format("%.2f", playingFund), sequence, getGameStatus(), betSize, WAIT);
        }

        String prediction = String.valueOf(combinedPrediction.first);

        String recommendedBet = Objects.equals(prediction, "p") ? "Player" : "Banker";

        return resolveBet(userInput, betSize, recommendedBet, predictedBet);
    }

    private GameResponse resolveBet(String userInput, double betSize, String recommendedBet, String predictedBet) {
        if (predictedBet.equals(WAIT) || predictedBet.isEmpty()) {
            return new GameResponse("Place your bet", sequence, getGameStatus(), betSize, recommendedBet);
        }

        String previousPrediction = predictedBet.equals("Player") ? "p" : "b";

        if (previousPrediction.equals(userInput)) {
            updateProfitAndFund(betSize, true);
            logger.info("You won! New profit: {}, New playing fund: {}", profit, playingFund);
            return new GameResponse("You won!", sequence, getGameStatus(), betSize, recommendedBet);
        } else {
            updateProfitAndFund(betSize, false);
            logger.info("You lost! New profit: {}, New playing fund: {}", profit, playingFund);
            return new GameResponse("You lost!", sequence, getGameStatus(), betSize, recommendedBet);
        }
    }

    private void updateProfitAndFund(double betSize, boolean isWin) {
        if (isWin) {
            profit += betSize;
            playingFund += betSize;
            totalWins++;
        } else {
            profit -= betSize;
            playingFund -= betSize;
            totalLosses++;
        }
    }

    @PostMapping("/back")
    public GameResponse back(@RequestParam String recommendedBet,
                             @RequestParam double baseBetAmount) {
        // Check if the sequence can be undone
//        if (sequence.length() <= 2) {
//            throw new IllegalStateException("You are no longer able to undo");
//        }

        // Log the initial state of the sequence


        // Remove the last two characters from the sequence if not empty
        // Get the last character to determine user input
//            char lastChar = sequence.charAt(sequence.length() - 1);
//            String userInput = String.valueOf(lastChar);
//
//            // Update the sequence by removing the last two characters
        logger.info("Current sequence before back: {}", sequence);
        sequence = sequence.substring(0, sequence.length() - 1); // Remove last two characters
//            logger.info("Updated sequence after back: {}", sequence);
        logger.info("Current sequence after back : {}", sequence);

        if (sequence.isEmpty()) {
            return reset();
        }
//            // Call the play method to reevaluate the game state based on the updated sequence
//            return play(userInput, recommendedBet, baseBetAmount);

        return new GameResponse("Removed previous result!", sequence, getGameStatus(), 0, WAIT);
    }


    @PostMapping("/reset")
    public GameResponse reset() {
        logger.info("Resetting game state to initial values.");
        // Reset all variables
//        journalService.saveJournal(new Journal(0, "", 0, getGameStatus().getHandCount(), getGameStatus().getProfit(), LocalDateTime.now(), LocalDate.now()));
//
        sequence = "";
        handCount = 0;
        totalWins = 0;
        totalLosses = 0;
        profit = 0;
        playingFund = INITIAL_PLAYING_FUND;
        waitingForVirtualWin = false;

        logger.info("Game state reset completed.");

        return new GameResponse("Game has been reset!", sequence, getGameStatus(), 0, null);
    }

    private Pair<Character, Double> combinePredictions(Pair<Character, Double> markovResult, String patternResult) {
        if (markovResult == null && patternResult == null) return new Pair<>(null, 0.0);
        if (markovResult == null) return new Pair<>(patternResult.charAt(0), 0.6);
        if (patternResult == null) return markovResult;

        char markovPrediction = markovResult.first, patternPrediction = patternResult.charAt(0);
        double markovConfidence = markovResult.second;

        return (markovPrediction == patternPrediction)
                ? new Pair<>(markovPrediction, Math.min(markovConfidence + 0.1, 1.0))
                : (markovConfidence >= 0.5 ? markovResult : new Pair<>(patternPrediction, 0.6));
    }

    private GameStatus getGameStatus() {
        return new GameStatus(handCount, totalWins, totalLosses, profit, playingFund);
    }

    @Getter
    public static class GameResponse {
        private final String message;
        private final String sequencePlayed;
        private final GameStatus status;
        private final double suggestedBet;
        private final String recommendedBet;

        public GameResponse(String message, String sequencePlayed, GameStatus status, double suggestedBet, String recommendedBet) {
            this.message = message;
            this.sequencePlayed = sequencePlayed;
            this.status = status;
            this.suggestedBet = suggestedBet;
            this.recommendedBet = recommendedBet;
        }
    }

    @Getter
    public static class GameStatus {
        private final int handCount;
        private final int totalWins;
        private final int totalLosses;
        private final double profit;
        private final double playingFund;

        public GameStatus(int handCount, int totalWins, int totalLosses, double profit, double playingFund) {
            this.handCount = handCount;
            this.totalWins = totalWins;
            this.totalLosses = totalLosses;
            this.profit = profit;
            this.playingFund = playingFund;
        }
    }
}
