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
    private static final int ZERO = 0;


    private static final Logger logger = LoggerFactory.getLogger(BaccaratController.class);
    private static final double STOP_PROFIT_PERCENTAGE = 0.40;
    private static final double STOP_LOSS_PERCENTAGE = 0.10;
    private static final double VIRTUAL_WIN_PROBABILITY = 0.5;
    private static double BASE_BET = 0.2;
    private static double INITIAL_PLAYING_FUND = 0.0;
    private static int MAX_DAILY_JOURNAL_LIMIT = 0;
    private final MarkovChain markovChain;
    private final PatternRecognizer patternRecognizer;
    private final JournalServiceImpl journalService;

    private final UserConfigService configService;
    private final String WAIT = "Wait..";
    private String sequence = ""; // Empty starting sequence
    private int handCount = 0;
    private int totalWins = 0;
    private int totalLosses = 0;
    private double profit = 0, playingFund = 0;
    private boolean waitingForVirtualWin = false;


    @Autowired
    public BaccaratController(MarkovChain markovChain, PatternRecognizer patternRecognizer, JournalServiceImpl journalService, UserConfigService configService) {
        this.markovChain = markovChain;
        this.patternRecognizer = patternRecognizer;
        this.journalService = journalService;
        this.configService = configService;

    }

    public static double getOnePercentOf(double number) {
        return number * 0.01;
    }

    //    @PostMapping("/init-config")
    public GameResponse initialize() {

        MAX_DAILY_JOURNAL_LIMIT = getDailyLimit();
        INITIAL_PLAYING_FUND = getPlayingFund();
        BASE_BET = getBaseBet();
        playingFund = INITIAL_PLAYING_FUND;

        GameStatus gameStatus = new GameStatus(ZERO, ZERO, ZERO, ZERO, playingFund);
        return new GameResponse("Game initialized!", sequence, gameStatus, BASE_BET, ZERO, INITIAL_PLAYING_FUND, WAIT);
    }

    @PostMapping("/play")
    public GameResponse play(@RequestParam String userInput, @RequestParam String recommendedBet, @RequestParam double baseBetAmount) {

        // Log input values
        logger.info("Received user input: {}", userInput);
        logger.info("Received recommendedBet input: {}", recommendedBet);
        logger.info("Received baseBetAmount input: {}", baseBetAmount);

        // Validate user input
        if (!isValidInput(userInput)) {
            return createErrorResponse();
        }
        BASE_BET = getBaseBet();

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();


        // Handle hand limits, stop profit/loss conditions
        if (hasReachedStopProfit()) {
            return saveAndReturn(new GameResponse(STOP_PROFIT_REACHED, sequence, getGameStatus(), BASE_BET, ZERO, INITIAL_PLAYING_FUND, WAIT));
        } else if (hasReachedStopLoss()) {
            return saveAndReturn(new GameResponse(STOP_LOSS_REACHED, sequence, getGameStatus(), BASE_BET, ZERO, INITIAL_PLAYING_FUND, WAIT));
        } else if (hasReachedDailyJournalLimit(userUuid)) {
            return saveAndReturn(new GameResponse(DAILY_LIMIT_REACHED, sequence, getGameStatus(), BASE_BET, ZERO, INITIAL_PLAYING_FUND, WAIT));
        }


        updateSequenceAndUpdateHandCount(userInput);
        // Process virtual win if applicable
        if (waitingForVirtualWin) {
            return processVirtualWin();
        }


        // Generate predictions using Markov chain and pattern recognition
        markovChain.train(sequence);
        Pair<Character, Double> markovPrediction = markovChain.predictNext(sequence.charAt(sequence.length() - 1));
        String patternPrediction = patternRecognizer.findPattern(sequence);

        logger.info("Markov Prediction: {}, Pattern Prediction: {}", markovPrediction, patternPrediction);

        // Combine predictions and handle the bet
        Pair<Character, Double> combinedPrediction = combinePredictions(markovPrediction, patternPrediction);
        return handleBet(userInput, combinedPrediction, recommendedBet, baseBetAmount);
    }

    private GameResponse saveAndReturn(GameResponse response) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        journalService.saveJournal(new Journal(ZERO, userUuid, ZERO, getGameStatus().getHandCount(), getGameStatus().getProfit(), LocalDateTime.now(), LocalDate.now()));
        return response;

    }

    private int getDailyLimit() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        // Continue with your logic using userUuid
        return configService.getConfigsByUserUuid(userUuid).stream().filter(config -> config.getName().equals(UserConfig.DAILY_LIMIT.getValue())).map(Config::getValue).map(Integer::parseInt).findFirst().orElse(0);

    }

    private double getPlayingFund() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        return configService.getConfigsByUserUuid(userUuid).stream().filter(config -> config.getName().equals(UserConfig.PLAYING_FUND.getValue())).map(Config::getValue).map(Double::parseDouble).findFirst().orElse(0.0);

    }

    private double getBaseBet() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        return configService.getConfigsByUserUuid(userUuid).stream().filter(config -> config.getName().equals(UserConfig.BASE_BET.getValue())).map(Config::getValue).map(Double::parseDouble).findFirst().orElse(0.0);

    }

    private boolean hasReachedDailyJournalLimit(String userUuid) {
        List<Journal> journals = journalService.getJournalsByUserUuidAndDateCreated(userUuid, LocalDate.now());
        return journals.size() >= MAX_DAILY_JOURNAL_LIMIT;
    }

    private GameResponse createErrorResponse() {
        logger.warn("Invalid input! Please enter 'p' for Player or 'b' for Banker.");
        GameStatus gameStatus = new GameStatus(0, 0, 0, 0, 0);
        return new GameResponse("Invalid input! Please enter 'p' for Player or 'b' for Banker.", sequence, gameStatus, BASE_BET, 0, INITIAL_PLAYING_FUND, WAIT);
    }

    private boolean isValidInput(String input) {
        if (!input.equals("p") && !input.equals("b")) {
            logger.warn("Invalid input received: {}. Must be 'p' or 'b'.", input);
            return false;
        }
        return true;
    }

    private void updateSequenceAndUpdateHandCount(String userInput) {
        sequence += userInput;
        handCount++;
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
            return new GameResponse("You won with virtual win!", sequence, getGameStatus(), BASE_BET, 0,INITIAL_PLAYING_FUND , WAIT);
        } else {
            updateProfitAndFund(BASE_BET, false);
            logger.info("Virtual win failed. New profit: {}, New playing fund: {}", profit, playingFund);
            return new GameResponse("Virtual win failed.", sequence, getGameStatus(), BASE_BET, 0, INITIAL_PLAYING_FUND, WAIT);
        }
    }

    private GameResponse handleBet(String userInput, Pair<Character, Double> combinedPrediction, String predictedBet, double betAmount) {

        if (combinedPrediction.first == null || combinedPrediction.second < 0.6) {
            logger.info("Prediction confidence too low. No bet suggested.");

            return new GameResponse("Prediction confidence too low, no bet suggested.", sequence, getGameStatus(), BASE_BET, 0,INITIAL_PLAYING_FUND , WAIT);
        }

        double betSize = betAmount * combinedPrediction.second * 5;

        if (playingFund < betSize) {
            logger.warn("Not enough funds to place bet. Current Playing Fund: ${}", playingFund);

            return new GameResponse("Not enough funds to place bet. Current Playing Fund: $" + String.format("%.2f", playingFund), sequence, getGameStatus(), BASE_BET, betSize, INITIAL_PLAYING_FUND, WAIT);
        }

        String prediction = String.valueOf(combinedPrediction.first);

        String recommendedBet = Objects.equals(prediction, "p") ? "Player" : "Banker";

        return resolveBet(userInput, betSize, recommendedBet, predictedBet);
    }

    private GameResponse resolveBet(String userInput, double betSize, String recommendedBet, String predictedBet) {
        if (predictedBet.equals(WAIT) || predictedBet.isEmpty()) {
            return new GameResponse("Place your bet", sequence, getGameStatus(), BASE_BET, betSize, INITIAL_PLAYING_FUND, recommendedBet);
        }

        String previousPrediction = predictedBet.equals("Player") ? "p" : "b";

        if (previousPrediction.equals(userInput)) {
            updateProfitAndFund(betSize, true);
            logger.info("You won! New profit: {}, New playing fund: {}", profit, playingFund);
            return new GameResponse("You won!", sequence, getGameStatus(), BASE_BET, betSize, INITIAL_PLAYING_FUND, recommendedBet);
        } else {
            updateProfitAndFund(betSize, false);
            logger.info("You lost! New profit: {}, New playing fund: {}", profit, playingFund);
            return new GameResponse("You lost!", sequence, getGameStatus(), BASE_BET, betSize,INITIAL_PLAYING_FUND, recommendedBet);
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

    @GetMapping("/current-state")
    public GameResponse getCurrentState(@RequestParam String message) {
        logger.info("Fetching current game state. " + message);

        double fund = getGameStatus().playingFund;
        if (fund == 0) {
            return initialize();
        }
        return new GameResponse(message, sequence, getGameStatus(), getBaseBet(), 0,INITIAL_PLAYING_FUND , WAIT);
    }

    @GetMapping("/recommended-base-bet-amount")
    public ResponseEntity<String> getRecommendedBaseBetAmount() {


        double fund = getPlayingFund();

        double recommendedBaseBet = getOnePercentOf(fund);


        return ResponseEntity.ok(String.valueOf(recommendedBaseBet));
    }

    @PostMapping("/playing-fund-config")
    public ResponseEntity<String> playingFundConfig(@RequestParam double playingFund) {
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
    public ResponseEntity<String> playingBaseBet(@RequestParam double baseBetAmount) {
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
        logger.info("Playing fund updated to: {} for user: {}", playingFund, userUuid);

        return ResponseEntity.ok("" + baseBetAmount);
    }


    @PostMapping("/initial-playing-fund")
    public ResponseEntity<String> initialPlayingFund(@RequestParam double initialPlayingFund) {
        String userUuid = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserUuid();
        logger.info("Received request to update playing fund for user: {}", userUuid);

        List<Config> userConfigList = configService.getConfigsByUserUuid(userUuid);

        Config playingFundConfig = null;
        if (userConfigList.stream().anyMatch(config -> config.getName().equals(UserConfig.PLAYING_FUND.getValue()))) {
            logger.info("User already has a playing fund configured. Updating existing value.");
            playingFundConfig = userConfigList.stream().filter(config -> config.getName().equals(UserConfig.PLAYING_FUND.getValue())).findFirst().orElse(null);
        }

        if (playingFundConfig == null) {
            logger.info("No existing playing fund. Creating a new one.");
            playingFundConfig = new Config(); // Initialize a new Config object if not found
            playingFundConfig.setName(UserConfig.PLAYING_FUND.getValue());
        }

        playingFundConfig.setValue(String.valueOf(initialPlayingFund));
        configService.saveOrUpdateConfig(playingFundConfig);
        logger.info("Playing fund updated to: {} for user: {}", playingFund, userUuid);

        INITIAL_PLAYING_FUND = Double.parseDouble(playingFundConfig.getValue());
        return ResponseEntity.ok("" + initialPlayingFund);
    }


    @PostMapping("/back")
    public GameResponse back(@RequestParam String recommendedBet, @RequestParam double baseBetAmount) {
        // Check if the sequence can be undone

//            // Update the sequence by removing the last two characters
        logger.info("Current sequence before back: {}", sequence);
        sequence = sequence.substring(0, sequence.length() - 1); // Remove last two characters
//            logger.info("Updated sequence after back: {}", sequence);
        logger.info("Current sequence after back : {}", sequence);

        if (sequence.isEmpty()) {
            return reset();
        }
        return new GameResponse("Removed previous result!", sequence, getGameStatus(), BASE_BET, 0,INITIAL_PLAYING_FUND , WAIT);
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
        playingFund = INITIAL_PLAYING_FUND;
        waitingForVirtualWin = false;

        initialize();

        logger.info("Game state reset completed.");
        return new GameResponse("Game has been reset!", sequence, getGameStatus(), BASE_BET, 0, INITIAL_PLAYING_FUND, WAIT);
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
        return new GameStatus(handCount, totalWins, totalLosses, profit, playingFund);
    }

    @Getter
    public static class GameResponse {
        private final String message;
        private final String sequencePlayed;
        private final GameStatus status;
        private final double baseBetAmount;
        private final double suggestedBet;
        private final double initialPlayingFund;
        private final String recommendedBet;

        public GameResponse(String message, String sequencePlayed, GameStatus status, double baseBetAmount, double suggestedBet, double initialPlayingFund, String recommendedBet) {
            this.message = message;
            this.sequencePlayed = sequencePlayed;
            this.status = status;
            this.baseBetAmount = baseBetAmount;
            this.suggestedBet = suggestedBet;
            this.initialPlayingFund = initialPlayingFund;
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
