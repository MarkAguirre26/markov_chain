package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.common.concrete.UserConfig;
import com.baccarat.markovchain.module.data.Config;
import com.baccarat.markovchain.module.data.Journal;
import com.baccarat.markovchain.module.model.Pair;
import com.baccarat.markovchain.module.model.UserPrincipal;
import com.baccarat.markovchain.module.services.impl.JournalServiceImpl;
import com.baccarat.markovchain.module.services.impl.MarkovChain;
import com.baccarat.markovchain.module.services.impl.PatternRecognizer;
import com.baccarat.markovchain.module.services.impl.UserConfigService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/baccarat")
public class BaccaratController {

    private static final String STOP_PROFIT_REACHED = "Stop profit reached! Restart the game.";
    private static final String STOP_LOSS_REACHED = "Stop loss reached! Restart the game.";
    private static final String DAILY_LIMIT_REACHED = "Daily limit reached! Please play again tomorrow.";
    private static final String MAX_HAND_LIMIT_REACHED = "Hands limit reached! Restart the game";
    private static final int ZERO = 0;


    private static final Logger logger = LoggerFactory.getLogger(BaccaratController.class);

    private static final double STOP_PROFIT_PERCENTAGE = 0.40;
    private static final double STOP_LOSS_PERCENTAGE = 0.10;
    private static final double VIRTUAL_WIN_PROBABILITY = 0.5;
    private static int BASE_BET_UNIT = 1;
    private static int INITIAL_PLAYING_UNITS = 0;
    private static int MAX_DAILY_JOURNAL_LIMIT = 0;
    private static final int MAX_HANDS = 60;

    private final MarkovChain markovChain;
    private final PatternRecognizer patternRecognizer;
    private final JournalServiceImpl journalService;

    private final UserConfigService configService;
    private final String WAIT = "Wait..";
    private String sequence = ""; // Empty starting sequence
    private int handCount = 0;
    private int totalWins = 0;
    private int totalLosses = 0;
    private int profit = 0, playingUnit = 0;

//    private boolean waitingForVirtualWin = false;


    @Autowired
    public BaccaratController(MarkovChain markovChain, PatternRecognizer patternRecognizer, JournalServiceImpl journalService, UserConfigService configService) {
        this.markovChain = markovChain;
        this.patternRecognizer = patternRecognizer;
        this.journalService = journalService;
        this.configService = configService;

    }

    public static int getOnePercentOf(int number) {
        return (int) (number * 0.01);
    }

    @PostMapping("/play")
    public GameResponse play(@RequestParam String userInput, @RequestParam String recommendedBet, @RequestParam int baseBetUnit) {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Log input values
        logger.info(userPrincipal.getUsername()+ ": Received user input: {}", userInput);
        logger.info(userPrincipal.getUsername()+ ": Received recommendedBet input: {}", recommendedBet);
        logger.info(userPrincipal.getUsername()+ ": Received baseBetAmount input: {}", baseBetUnit);

        if(hasReachedDailyJournalLimit(userPrincipal.getUserUuid())){
            return new GameResponse(DAILY_LIMIT_REACHED, sequence, getGameStatus(), BASE_BET_UNIT, ZERO, INITIAL_PLAYING_UNITS, WAIT);

        }


        // Validate user input
        if (!isValidInput(userInput)) {
            return createErrorResponse();
        }

        updateSequenceAndUpdateHandCount(userInput,userPrincipal.getUsername());

        // Process virtual win if applicable
//        if (waitingForVirtualWin) {
//            return processVirtualWin();
//        }


        // Generate predictions using Markov chain and pattern recognition
        markovChain.train(sequence);
        Pair<Character, Double> markovPrediction = markovChain.predictNext(sequence.charAt(sequence.length() - 1));
        String patternPrediction = patternRecognizer.findPattern(sequence);

        logger.info(userPrincipal.getUsername()+": Markov Prediction: {}, Pattern Prediction: {}", markovPrediction, patternPrediction);

        // Combine predictions and handle the bet
        Pair<Character, Double> combinedPrediction = combinePredictions(markovPrediction, patternPrediction);
        return handleBet(userPrincipal,userInput, combinedPrediction, recommendedBet, baseBetUnit);
    }

    //    @PostMapping("/init-config")
    public GameResponse initialize() {

        MAX_DAILY_JOURNAL_LIMIT = getDailyLimit();
        INITIAL_PLAYING_UNITS = getPlayingUnit();
        BASE_BET_UNIT = getBaseBetUnit();
        playingUnit = INITIAL_PLAYING_UNITS;

        GameStatus gameStatus = new GameStatus(ZERO, ZERO, ZERO, ZERO, playingUnit);
        return new GameResponse("Game initialized!", sequence, gameStatus, BASE_BET_UNIT, ZERO, INITIAL_PLAYING_UNITS, WAIT);
    }

    private GameResponse saveAndReturn(GameResponse response) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        journalService.saveJournal(new Journal(ZERO, userUuid, ZERO, response.status.getHandCount(), response.status.getProfit(), LocalDateTime.now(), LocalDate.now()));
        return response;

    }

    private int getDailyLimit() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        // Continue with your logic using userUuid
        return configService.getConfigsByUserUuid(userUuid).stream().filter(config -> config.getName().equals(UserConfig.DAILY_LIMIT.getValue())).map(Config::getValue).map(Integer::parseInt).findFirst().orElse(0);

    }

    private int getPlayingUnit() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        return configService.getConfigsByUserUuid(userUuid).stream().filter(config -> config.getName().equals(UserConfig.PLAYING_FUND.getValue())).map(Config::getValue).map(Integer::parseInt).findFirst().orElse(0);

    }

    private int getBaseBetUnit() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        return configService.getConfigsByUserUuid(userUuid).stream().filter(config -> config.getName().equals(UserConfig.BASE_BET.getValue())).map(Config::getValue).map(Integer::parseInt).findFirst().orElse(0);

    }

    private boolean hasReachedDailyJournalLimit(String userUuid) {
        List<Journal> journals = journalService.getJournalsByUserUuidAndDateCreated(userUuid, LocalDate.now());
        return journals.size() >= MAX_DAILY_JOURNAL_LIMIT;
    }

    private GameResponse createErrorResponse() {
        logger.warn("Invalid input! Please enter 'p' for Player or 'b' for Banker.");
        GameStatus gameStatus = new GameStatus(0, 0, 0, 0, 0);
        return new GameResponse("Invalid input! Please enter 'p' for Player or 'b' for Banker.", sequence, gameStatus, BASE_BET_UNIT, 0, INITIAL_PLAYING_UNITS, WAIT);
    }

    private boolean isValidInput(String input) {
        if (!input.equals("p") && !input.equals("b")) {
            logger.warn("Invalid input received: {}. Must be 'p' or 'b'.", input);
            return false;
        }
        return true;
    }

    private void updateSequenceAndUpdateHandCount(String userInput,String username) {
        sequence += userInput;
        handCount++;
        logger.info(username+ ": Sequence: {}", sequence);
    }

    private boolean hasReachedStopProfit() {
        if (profit >= STOP_PROFIT_PERCENTAGE * INITIAL_PLAYING_UNITS) {

            return true;
        }
        return false;
    }

    private boolean hasReachedStopLoss() {
        if (profit <= -STOP_LOSS_PERCENTAGE * INITIAL_PLAYING_UNITS) {

            return true;
        }
        return false;
    }

    private boolean hasReachedHandsLimit() {
        return handCount >= MAX_HANDS;
    }

//    private GameResponse processVirtualWin() {
//        waitingForVirtualWin = false;
//        boolean isVirtualWin = Math.random() < VIRTUAL_WIN_PROBABILITY;
//
//        if (isVirtualWin) {
//            updateProfitAndFund(BASE_BET_UNIT, true);
//            logger.info("Virtual win! New profit: {}, New playing fund: {}", profit, playingUnit);
//            return new GameResponse("You won with virtual win!", sequence, getGameStatus(), BASE_BET_UNIT, 0, INITIAL_PLAYING_UNITS, WAIT);
//        } else {
//            updateProfitAndFund(BASE_BET_UNIT, false);
//            logger.info("Virtual win failed. New profit: {}, New playing fund: {}", profit, playingUnit);
//            return new GameResponse("Virtual win failed.", sequence, getGameStatus(), BASE_BET_UNIT, 0, INITIAL_PLAYING_UNITS, WAIT);
//        }
//    }

    private GameResponse handleBet(UserPrincipal userPrincipal, String userInput, Pair<Character, Double> combinedPrediction, String predictedBet, int betUnit) {
    String username = userPrincipal.getUsername();
        if (combinedPrediction.first == null || combinedPrediction.second < 0.6) {
            logger.info(username+": Prediction confidence too low. No bet suggested.");
            return new GameResponse("Prediction confidence too low, no bet suggested.", sequence, getGameStatus(), BASE_BET_UNIT, 0, INITIAL_PLAYING_UNITS, WAIT);
        }

        int betSize = (int) Math.ceil(betUnit * combinedPrediction.second * 5);


        if (playingUnit < betSize) {
            logger.warn(username+": Not enough funds to place bet. Current Playing Fund: ${}", playingUnit);
            return new GameResponse("Not enough funds to place bet. Current Playing Fund: $" + String.format("%d", playingUnit), sequence, getGameStatus(), BASE_BET_UNIT, betSize, INITIAL_PLAYING_UNITS, WAIT);
        }

        String prediction = String.valueOf(combinedPrediction.first);

        String recommendedBet = Objects.equals(prediction, "p") ? "Player" : "Banker";

        return resolveBet(userPrincipal,userInput, betSize, recommendedBet, predictedBet);
    }

    private GameResponse resolveBet(UserPrincipal userPrincipal,String userInput, int betSize, String recommendedBet, String predictedBet) {
       String username = userPrincipal.getUsername();
        if (predictedBet.equals(WAIT) || predictedBet.isEmpty()) {
            return new GameResponse("Place your bet", sequence, getGameStatus(), BASE_BET_UNIT, betSize, INITIAL_PLAYING_UNITS, recommendedBet);
        }

        String previousPrediction = predictedBet.equals("Player") ? "p" : "b";

        if (previousPrediction.equals(userInput)) {
            updateProfitAndFund(betSize, true);
            logger.info(username+": You won! New profit: {}, New playing fund: {}", profit, playingUnit);
            // Handle hand limits, stop profit/loss conditions
            return settleLimitAndValidation(userPrincipal, betSize, recommendedBet, true);
        } else {
            updateProfitAndFund(betSize, false);
            logger.info(username+": You lost! New profit: {}, New playing fund: {}", profit, playingUnit);

            return settleLimitAndValidation(userPrincipal,betSize, recommendedBet, false);
        }


    }

    private GameResponse settleLimitAndValidation(UserPrincipal userPrincipal, int betSize, String recommendedBet, boolean isWon) {

        String username = userPrincipal.getUsername();
        String userUuid = userPrincipal.getUserUuid();

        if (hasReachedStopProfit()) {
            logger.warn(username+": Reached stop profit. New profit: {}, New playing fund: {}", profit, playingUnit);
            return saveAndReturn(new GameResponse(STOP_PROFIT_REACHED, sequence, getGameStatus(), BASE_BET_UNIT, ZERO, INITIAL_PLAYING_UNITS, WAIT));
        } else if (hasReachedStopLoss()) {
            logger.warn(username+": Reached stop loss. New profit: {}, New playing fund: {}", profit, playingUnit);
            return saveAndReturn(new GameResponse(STOP_LOSS_REACHED, sequence, getGameStatus(), BASE_BET_UNIT, ZERO, INITIAL_PLAYING_UNITS, WAIT));
        } else if (hasReachedHandsLimit()) {
            logger.warn(username+": Reached max hand limit. New profit: {}, New playing fund: {}", profit, playingUnit);
            return saveAndReturn(new GameResponse(MAX_HAND_LIMIT_REACHED, sequence, getGameStatus(), BASE_BET_UNIT, ZERO, INITIAL_PLAYING_UNITS, WAIT));
        } else {
            return new GameResponse(isWon ? "You won!" : "You lost!", sequence, getGameStatus(), BASE_BET_UNIT, betSize, INITIAL_PLAYING_UNITS, recommendedBet);
        }
    }

    private void updateProfitAndFund(double betSize, boolean isWin) {
        if (isWin) {
            profit += betSize;
            playingUnit += betSize;
            totalWins++;
        } else {
            profit -= betSize;
            playingUnit -= betSize;
            totalLosses++;
        }
    }

    @GetMapping("/current-state")
    public GameResponse getCurrentState(@RequestParam String message) {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        logger.info(userPrincipal.getUsername()+ ": Fetching current game state. " + message);

        double fund = getGameStatus().playingFund;
        if (fund == 0) {
            return initialize();
        }
        return new GameResponse(message, sequence, getGameStatus(), getBaseBetUnit(), 0, INITIAL_PLAYING_UNITS, WAIT);
    }

    @GetMapping("/recommended-base-bet-amount")
    public ResponseEntity<String> getRecommendedBaseBetAmount() {


        int fund = getPlayingUnit();

        int recommendedBaseBetUnit = getOnePercentOf(fund);


        return ResponseEntity.ok(String.valueOf(recommendedBaseBetUnit));
    }

    @PostMapping("/playing-fund-config")
    public ResponseEntity<String> playingFundConfig(@RequestParam int playingFund) {
        String userUuid = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserUuid();
        logger.info("Received request to update playing fund for user: {}", userUuid);

        List<Config> userConfigList = configService.getConfigsByUserUuid(userUuid);

        Config playingFundConfig = null;
        if (userConfigList.stream().anyMatch(config -> config.getName().equals(UserConfig.PLAYING_FUND.getValue()))) {
            logger.info("User already has a playing fund configured. Updating existing value.");
            playingFundConfig = userConfigList.stream().filter(config -> config.getName().equals(UserConfig.PLAYING_FUND.getValue())).findFirst().orElse(null);
        }

        if (playingFundConfig == null) {
            logger.info("No existing playing fund found. Creating a new one.");
            playingFundConfig = new Config(); // Initialize a new Config object if not found
            playingFundConfig.setName(UserConfig.PLAYING_FUND.getValue());
        }

        playingFundConfig.setValue(String.valueOf(playingFund));
        configService.saveOrUpdateConfig(playingFundConfig);
        logger.info("Playing fund updated to: {} for user: {}", playingFund, userUuid);

        return ResponseEntity.ok("Playing fund updated successfully");
    }


    @PostMapping("/playing-base-bet")
    public ResponseEntity<String> playingBaseBet(@RequestParam int baseBetAmount) {
        String userUuid = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserUuid();
        logger.info("Received request to update base bet for user: {}", userUuid);

        List<Config> userConfigList = configService.getConfigsByUserUuid(userUuid);

        Config playingBaseBetConfig = null;
        if (userConfigList.stream().anyMatch(config -> config.getName().equals(UserConfig.BASE_BET.getValue()))) {
            logger.info("User already has a base bet configured. Updating existing value.");
            playingBaseBetConfig = userConfigList.stream().filter(config -> config.getName().equals(UserConfig.BASE_BET.getValue())).findFirst().orElse(null);
        }

        if (playingBaseBetConfig == null) {
            logger.info("No existing base bet. Creating a new one.");
            playingBaseBetConfig = new Config(); // Initialize a new Config object if not found
            playingBaseBetConfig.setName(UserConfig.BASE_BET.getValue());
        }

        playingBaseBetConfig.setValue(String.valueOf(baseBetAmount));
        configService.saveOrUpdateConfig(playingBaseBetConfig);
        logger.info("Playing fund updated to: {} for user: {}", playingUnit, userUuid);

        return ResponseEntity.ok("" + baseBetAmount);
    }


//    @PostMapping("/initial-playing-fund")
//    public ResponseEntity<String> initialPlayingFund(@RequestParam int initialPlayingUnit) {
//        String userUuid = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserUuid();
//        logger.info("Received request to update playing fund for user: {}", userUuid);
//
//        List<Config> userConfigList = configService.getConfigsByUserUuid(userUuid);
//
//        Config playingFundConfig = null;
//        if (userConfigList.stream().anyMatch(config -> config.getName().equals(UserConfig.PLAYING_FUND.getValue()))) {
//            logger.info("User already has a playing fund configured. Updating existing value.");
//            playingFundConfig = userConfigList.stream().filter(config -> config.getName().equals(UserConfig.PLAYING_FUND.getValue())).findFirst().orElse(null);
//        }
//
//        if (playingFundConfig == null) {
//            logger.info("No existing playing fund. Creating a new one.");
//            playingFundConfig = new Config(); // Initialize a new Config object if not found
//            playingFundConfig.setName(UserConfig.PLAYING_FUND.getValue());
//        }
//
//        playingFundConfig.setValue(String.valueOf(initialPlayingUnit));
//        configService.saveOrUpdateConfig(playingFundConfig);
//        logger.info("Playing fund updated to: {} for user: {}", playingUnit, userUuid);
//
//        INITIAL_PLAYING_UNITS = Integer.valueOf(playingFundConfig.getValue());
//        return ResponseEntity.ok("" + initialPlayingUnit);
//    }


    @PostMapping("/back")
    public GameResponse back(@RequestParam String userInput, @RequestParam String recommendedBet) {
        // Check if the sequence can be undone

//            // Update the sequence by removing the last two characters
        logger.info("Current sequence before back: {}", sequence);
        sequence = sequence.substring(0, sequence.length() - 1); // Remove last two characters
//            logger.info("Updated sequence after back: {}", sequence);
        logger.info("Current sequence after back : {}", sequence);

        if (sequence.isEmpty()) {
            return reset();
        }
        return new GameResponse("Removed previous result!", sequence, getGameStatus(), BASE_BET_UNIT, 0, INITIAL_PLAYING_UNITS, WAIT);
    }


    @PostMapping("/reset")
    public GameResponse reset() {
        logger.info("Resetting game state to initial values.");
        // Reset all variables
        sequence = "";
        handCount = 0;
        totalWins = 0;
        totalLosses = 0;
        profit = 0;
        playingUnit = INITIAL_PLAYING_UNITS;
//        waitingForVirtualWin = false;

        initialize();

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info(userPrincipal.getUsername()+": Game state reset completed.");
        return new GameResponse("Game has been reset!", sequence, getGameStatus(), BASE_BET_UNIT, 0, INITIAL_PLAYING_UNITS, WAIT);
    }


    private Pair<Character, Double> combinePredictions(Pair<Character, Double> markovResult, String patternResult) {
        if (markovResult == null && patternResult == null) return new Pair<>(null, 0.0);
        if (markovResult == null) return new Pair<>(patternResult.charAt(0), 0.6);
        if (patternResult == null) return markovResult;

        char markovPrediction = markovResult.first, patternPrediction = patternResult.charAt(0);
        double markovConfidence = markovResult.second;

        return (markovPrediction == patternPrediction) ? new Pair<>(markovPrediction, Math.min(markovConfidence + 0.1, 1.0)) : (markovConfidence >= 0.5 ? markovResult : new Pair<>(patternPrediction, 0.6));
    }

    private GameStatus getGameStatus() {
        return new GameStatus(handCount, totalWins, totalLosses, profit, playingUnit);
    }

    @Getter
    public static class GameResponse {
        private final String message;
        private final String sequencePlayed;
        private final GameStatus status;
        private final int baseBetUnit;
        private final int suggestedBet;
        private final int initialPlayingUnit;
        private final String recommendedBet;

        public GameResponse(String message, String sequencePlayed, GameStatus status, int baseBetUnit, int suggestedBet, int initialPlayingUnit, String recommendedBet) {
            this.message = message;
            this.sequencePlayed = sequencePlayed;
            this.status = status;
            this.baseBetUnit = baseBetUnit;
            this.suggestedBet = suggestedBet;
            this.initialPlayingUnit = initialPlayingUnit;
            this.recommendedBet = recommendedBet;
        }
    }

    @Getter
    public static class GameStatus {

        private final int handCount;
        private final int totalWins;
        private final int totalLosses;
        private final int profit;
        private final int playingFund;

        public GameStatus(int handCount, int totalWins, int totalLosses, int profit, int playingFund) {
            this.handCount = handCount;
            this.totalWins = totalWins;
            this.totalLosses = totalLosses;
            this.profit = profit;
            this.playingFund = playingFund;
        }
    }
}
