package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.common.concrete.UserConfig;
import com.baccarat.markovchain.module.data.Config;
import com.baccarat.markovchain.module.data.GameResponse;
import com.baccarat.markovchain.module.data.GameStatus;
import com.baccarat.markovchain.module.data.Journal;
import com.baccarat.markovchain.module.data.response.GameResultResponse;
import com.baccarat.markovchain.module.data.response.GameResultStatus;
import com.baccarat.markovchain.module.model.Pair;
import com.baccarat.markovchain.module.model.UserPrincipal;
import com.baccarat.markovchain.module.services.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/baccarat")
public class BaccaratController {

    private static final String STOP_PROFIT_REACHED = "Stop profit reached! Restart the game.";
    private static final String STOP_LOSS_REACHED = "Stop loss reached! Restart the game.";
    private static final String DAILY_LIMIT_REACHED = "Daily limit! Please play again tomorrow.";
    private static final String MAX_HAND_LIMIT_REACHED = "Hands limit reached! Restart the game";
    private static final int ZERO = 0;


    private static final Logger logger = LoggerFactory.getLogger(BaccaratController.class);

    private static final double STOP_PROFIT_PERCENTAGE = 0.40;
    private static final double STOP_LOSS_PERCENTAGE = 0.10;
    private static final int MAX_HANDS = 60;

    private final MarkovChain markovChain;
    private final PatternRecognizer patternRecognizer;
    private final JournalServiceImpl journalService;
    private final GameStatusService gameStatusService;
    private final GameResponseService gameResponseService;

    private final UserConfigService configService;
    private final String WAIT = "Wait..";


    @Autowired
    public BaccaratController(MarkovChain markovChain, PatternRecognizer patternRecognizer, JournalServiceImpl journalService, GameStatusService gameStatusService, GameResponseService gameResponseService, UserConfigService configService) {
        this.markovChain = markovChain;
        this.patternRecognizer = patternRecognizer;
        this.journalService = journalService;
        this.gameStatusService = gameStatusService;
        this.gameResponseService = gameResponseService;
        this.configService = configService;

    }

    public static int getOnePercentOf(int number) {
        return (int) (number * 0.01);
    }

    @PostMapping("/play")
    public GameResultResponse play(@RequestParam String userInput, @RequestParam String recommendedBet, @RequestParam int baseBetUnit) {

        initialize();

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        GameResultResponse gameResultResponseInitial = getGameResponse();

        logger.info(userPrincipal.getUsername() + ": Received user input: {}", userInput);
        logger.info(userPrincipal.getUsername() + ": Received recommendedBet input: {}", recommendedBet);
        logger.info(userPrincipal.getUsername() + ": Received baseBetAmount input: {}", baseBetUnit);

        if (hasReachedDailyJournalLimit()) {
            gameResultResponseInitial.setDailyLimitReached(true);
            gameResultResponseInitial.setMessage(DAILY_LIMIT_REACHED);
            gameResultResponseInitial.setRecommendedBet(recommendedBet);
            return provideGameResponse(gameResultResponseInitial);
        }

        GameResultResponse gameResultResponse = updateSequenceAndUpdateHandCount(gameResultResponseInitial, userInput);


        // Generate predictions using Markov chain and pattern recognition
        String sequence = gameResultResponse.getSequence();
        markovChain.train(sequence);
        Pair<Character, Double> markovPrediction = markovChain.predictNext(sequence.charAt(sequence.length() - 1));
        String patternPrediction = patternRecognizer.findPattern(sequence);

        logger.info(userPrincipal.getUsername() + ": Markov Prediction: {}, Pattern Prediction: {}", markovPrediction, patternPrediction);

        // Combine predictions and handle the bet
        Pair<Character, Double> combinedPrediction = combinePredictions(markovPrediction, patternPrediction);

        return handleBet(gameResultResponse, userPrincipal, userInput, combinedPrediction, recommendedBet, baseBetUnit);
    }

    //        @PostMapping("/init-config")
    public void initialize() {


        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        com.baccarat.markovchain.module.data.GameResponse gameResponse = gameResponseService.getGameResponseByUserUuid(userPrincipal.getUserUuid());

        if (gameResponse == null) {

            gameResponse = new com.baccarat.markovchain.module.data.GameResponse();
            gameResponse.setUserUuid(userPrincipal.getUserUuid());
            gameResponse.setBaseBetUnit(1);
            gameResponse.setSuggestedBetUnit(1);
            gameResponse.setInitialPlayingUnits(100);
            gameResponse.setRecommendedBet(WAIT);
            gameResponse.setSequence("1111");
            gameResponse.setMessage("Game initialized!");
            gameResponseService.createOrUpdateGameResponse(gameResponse);

        }

        Optional<GameStatus> gameStatus = gameStatusService.findByUserUuid(userPrincipal.getUserUuid());
        if (gameStatus.isEmpty()) {
            GameStatus s = new GameStatus();
            s.setUserUuid(userPrincipal.getUserUuid());
            s.setHandCount(0);
            s.setWins(0);
            s.setLosses(0);
            s.setProfit(0);
            s.setPlayingUnits(100);
            s.setDateLastUpdated(LocalDateTime.now());
            gameStatusService.save(s);
        }

//        return provideGameResponse(gameResponse.getMessage(), gameResponse.getSequence(), WAIT);
    }

    private GameResultResponse saveAndReturn(GameResultResponse response) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        journalService.saveJournal(new Journal(ZERO, userUuid, ZERO, response.getGameStatus().getHandCount(), response.getGameStatus().getProfit(), LocalDateTime.now(), LocalDate.now()));
        return response;

    }

    private int getDailyLimit() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        // Continue with your logic using userUuid
        return configService.getConfigsByUserUuid(userUuid).stream().filter(config -> config.getName().equals(UserConfig.DAILY_LIMIT.getValue())).map(Config::getValue).map(Integer::parseInt).findFirst().orElse(0);

    }


    private boolean hasReachedDailyJournalLimit() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Journal> journals = journalService.getJournalsByUserUuidAndDateCreated(userPrincipal.getUserUuid(), LocalDate.now());
        int maxDailyJournalLimit = getDailyLimit();
        return journals.size() >= maxDailyJournalLimit;
    }

    private GameResultResponse createErrorResponse() {
        logger.warn("Invalid input! Please enter 'p' for Player or 'b' for Banker.");
        // GameStatus gameStatus = new GameStatus(0, 0, 0, 0, 0);
        GameResultResponse gameResultResponse = getGameResponse();

        gameResultResponse.setMessage("Invalid input! Please enter 'p' for Player or 'b' for Banker.");
        gameResultResponse.setSequence(gameResultResponse.getSequence());
        gameResultResponse.setRecommendedBet(WAIT);

        return provideGameResponse(gameResultResponse);
    }


    private GameResultResponse updateSequenceAndUpdateHandCount(GameResultResponse gameResultResponse, String userInput) {

        // Fetch the current game response once

        GameResultStatus gameStatus = gameResultResponse.getGameStatus();
        // Update the sequence and hand count
        String sequence = gameResultResponse.getSequence() == null ? "1111" : gameResultResponse.getSequence() + userInput;
        int handCount = gameStatus == null ? 1 : gameStatus.getHandCount() + 1;

        assert gameStatus != null;

        gameStatus.setHandCount(handCount);
        gameResultResponse.setGameStatus(gameStatus);
        gameResultResponse.setSequence(sequence);

        return gameResultResponse;
    }

    private boolean hasReachedStopProfit(GameResultResponse gameResultResponse) {

//        GameResultResponse gameResultResponse = getGameResponse();
        int profit = gameResultResponse.getGameStatus().getProfit();
        if (profit >= STOP_PROFIT_PERCENTAGE * gameResultResponse.getInitialPlayingUnits()) {

            return true;
        }
        return false;
    }

    private boolean hasReachedStopLoss(GameResultResponse gameResultResponse) {
//        GameResultResponse gameResultResponse = getGameResponse();
        int profit = gameResultResponse.getGameStatus().getProfit();

        if (profit <= -STOP_LOSS_PERCENTAGE * gameResultResponse.getInitialPlayingUnits()) {

            return true;
        }
        return false;
    }

    private boolean hasReachedHandsLimit(GameResultResponse gameResultResponse) {
//        GameResultResponse gameResultResponse = getGameResponse();
        int handCount = gameResultResponse.getGameStatus().getHandCount(); // th is because the first hand is not counted. Do not touch this
        int sequenceLength = gameResultResponse.getSequence().length();
        return handCount >= MAX_HANDS || sequenceLength >= MAX_HANDS;
    }


    private GameResultResponse handleBet(GameResultResponse gameResultResponse,
                                         UserPrincipal userPrincipal,
                                         String userInput,
                                         Pair<Character, Double> combinedPrediction,
                                         String predictedBet,
                                         int betUnit) {


        String username = userPrincipal.getUsername();

        int betSize = (int) Math.ceil(betUnit * combinedPrediction.second * 5);
        int playingUnit = gameResultResponse.getGameStatus().getPlayingUnits();


        if (combinedPrediction.first == null || combinedPrediction.second < 0.6) {


            logger.info(username + ": Prediction confidence too low. No bet suggested.");
            gameResultResponse.setMessage("Prediction confidence too low, no bet suggested.");

            gameResultResponse.setSequence(gameResultResponse.getSequence());
            gameResultResponse.setRecommendedBet(WAIT);

//            return provideGameResponse(gameResultResponse);
            return validateGameResult(predictedBet, userInput, username, gameResultResponse, betSize, gameResultResponse.getRecommendedBet());

        }


        if (playingUnit < betSize) {

            logger.warn(username + ": Not enough funds to place bet. Current Playing Fund: ${}", playingUnit);
            gameResultResponse.setMessage("Not enough funds to place bet. Current Playing Fund: $" + String.format("%d", playingUnit));
            gameResultResponse.setSequence(gameResultResponse.getSequence());
            gameResultResponse.setRecommendedBet(WAIT);

//            return provideGameResponse(gameResultResponse);
            return validateGameResult(predictedBet, userInput, username, gameResultResponse, betSize, gameResultResponse.getRecommendedBet());
        }

        String prediction = String.valueOf(combinedPrediction.first);
        String recommendedBet = Objects.equals(prediction, "p") ? "Player" : "Banker";
        return resolveBet(gameResultResponse, userPrincipal, userInput, betSize, recommendedBet, predictedBet);
    }

    private GameResultResponse resolveBet(GameResultResponse gameResultResponse,
                                          UserPrincipal userPrincipal,
                                          String userInput,
                                          int betSize,
                                          String recommendedBet,
                                          String predictedBet) {

        String username = userPrincipal.getUsername();

        gameResultResponse.setDailyLimitReached(hasReachedDailyJournalLimit());

        if (predictedBet.equals(WAIT) || predictedBet.isEmpty()) {
            gameResultResponse.setMessage("Place your bet");
            gameResultResponse.setRecommendedBet(recommendedBet);
            return provideGameResponse(gameResultResponse);
        }

        return validateGameResult(predictedBet, userInput, username, gameResultResponse, betSize, recommendedBet);

//        String previousPrediction = predictedBet.equals("Player") ? "p" : "b";
//        if (previousPrediction.equals(userInput)) {
//            logger.info(username + ": You won!");
//            return settleLimitAndValidation(updateProfitAndFund(gameResultResponse, betSize, true), recommendedBet, true);
//        } else {
//            logger.info(username + ": You lost!");
//            return settleLimitAndValidation(updateProfitAndFund(gameResultResponse, betSize, false), recommendedBet, false);
//        }

    }

    private GameResultResponse validateGameResult(String predictedBet, String userInput, String username,
                                                  GameResultResponse gameResultResponse, int betSize, String recommendedBet) {
        String previousPrediction = predictedBet.equals("Player") ? "p" : "b";
        if (previousPrediction.equals(userInput)) {
            logger.info(username + ": You won!");
            return settleLimitAndValidation(updateProfitAndFund(gameResultResponse, betSize, true), recommendedBet, true);
        } else {
            logger.info(username + ": You lost!");
            return settleLimitAndValidation(updateProfitAndFund(gameResultResponse, betSize, false), recommendedBet, false);
        }
    }

    private GameResultResponse settleLimitAndValidation(GameResultResponse gameResultResponse, String recommendedBet, boolean isWon) {


        int profit = gameResultResponse.getGameStatus().getProfit();
        int playingUnit = gameResultResponse.getGameStatus().getPlayingUnits();
//        String sequence = gameResultResponse.getSequence();

        gameResultResponse.setSequence(gameResultResponse.getSequence());

        gameResultResponse.setRecommendedBet(WAIT);
        gameResultResponse.setDailyLimitReached(hasReachedDailyJournalLimit());

        if (hasReachedStopProfit(gameResultResponse)) {
            logger.warn(": Reached stop profit. New profit: {}, New playing fund: {}", profit, playingUnit);
            gameResultResponse.setMessage(STOP_PROFIT_REACHED);
            return saveAndReturn(provideGameResponse(gameResultResponse));
        } else if (hasReachedStopLoss(gameResultResponse)) {
            logger.warn(": Reached stop loss. New profit: {}, New playing fund: {}", profit, playingUnit);
            gameResultResponse.setMessage(STOP_LOSS_REACHED);
            return saveAndReturn(provideGameResponse(gameResultResponse));
        } else if (hasReachedHandsLimit(gameResultResponse)) {
            logger.warn(": Reached max hand limit. New profit: {}, New playing fund: {}", profit, playingUnit);
            gameResultResponse.setMessage(MAX_HAND_LIMIT_REACHED);
            return saveAndReturn(provideGameResponse(gameResultResponse));
        } else {
            gameResultResponse.setMessage(isWon ? "You won!" : "You lost!");
            gameResultResponse.setRecommendedBet(recommendedBet);
            return provideGameResponse(gameResultResponse);
        }

    }

    private GameResultResponse updateProfitAndFund(GameResultResponse gameResultResponse, int betSize, boolean isWin) {

        int profit = gameResultResponse.getGameStatus().getProfit();
        int playingUnit = gameResultResponse.getGameStatus().getPlayingUnits();
        int totalWins = gameResultResponse.getGameStatus().getWins();
        int totalLosses = gameResultResponse.getGameStatus().getLosses();

        if (isWin) {
            profit += betSize;
            playingUnit += betSize;
            totalWins++;
        } else {
            profit -= betSize;
            playingUnit -= betSize;
            totalLosses++;
        }

        GameResultStatus gameResultStatus = gameResultResponse.getGameStatus();
        gameResultStatus.setProfit(profit);
        gameResultStatus.setPlayingUnits(playingUnit);
        gameResultStatus.setWins(totalWins);
        gameResultStatus.setLosses(totalLosses);

        gameResultResponse.setGameStatus(gameResultStatus);
        gameResultResponse.setSuggestedBetUnit(betSize);

        return gameResultResponse;
    }

    @GetMapping("/current-state")
    public GameResultResponse getCurrentState(@RequestParam String message) {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        logger.info(userPrincipal.getUsername() + ": Fetching current game state. " + message);

        return getGameResponse();
    }


//    @PostMapping("/playing-fund-config")
//    public ResponseEntity<String> playingFundConfig(@RequestParam int playingFund) {
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
//            logger.info("No existing playing fund found. Creating a new one.");
//            playingFundConfig = new Config(); // Initialize a new Config object if not found
//            playingFundConfig.setName(UserConfig.PLAYING_FUND.getValue());
//        }
//
//        playingFundConfig.setValue(String.valueOf(playingFund));
//        configService.saveOrUpdateConfig(playingFundConfig);
//        logger.info("Playing fund updated to: {} for user: {}", playingFund, userUuid);
//
//        return ResponseEntity.ok("Playing fund updated successfully");
//    }


//    @PostMapping("/playing-base-bet")
//    public ResponseEntity<String> playingBaseBet(@RequestParam int baseBetAmount) {
//        String userUuid = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserUuid();
//        logger.info("Received request to update base bet for user: {}", userUuid);
//
//        List<Config> userConfigList = configService.getConfigsByUserUuid(userUuid);
//
//        Config playingBaseBetConfig = null;
//        if (userConfigList.stream().anyMatch(config -> config.getName().equals(UserConfig.BASE_BET.getValue()))) {
//            logger.info("User already has a base bet configured. Updating existing value.");
//            playingBaseBetConfig = userConfigList.stream().filter(config -> config.getName().equals(UserConfig.BASE_BET.getValue())).findFirst().orElse(null);
//        }
//
//        if (playingBaseBetConfig == null) {
//            logger.info("No existing base bet. Creating a new one.");
//            playingBaseBetConfig = new Config(); // Initialize a new Config object if not found
//            playingBaseBetConfig.setName(UserConfig.BASE_BET.getValue());
//        }
//
//        playingBaseBetConfig.setValue(String.valueOf(baseBetAmount));
//        configService.saveOrUpdateConfig(playingBaseBetConfig);
////        logger.info("Playing fund updated to: {} for user: {}", playingUnit, userUuid);
//
//        return ResponseEntity.ok("" + baseBetAmount);
//    }


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
    public GameResultResponse back() {
        // Check if the sequence can be undone

        GameResultResponse gameResultResponse = getGameResponse();
        GameResultStatus gameResultStatus = gameResultResponse.getGameStatus();

        if (gameResultStatus.getHandCount() == 1) {
            gameResultResponse.setMessage("You may use RESET button.");
            return gameResultResponse;
        }

        String sequence = gameResultResponse.getSequence();

//            // Update the sequence by removing the last two characters
        logger.info("Current sequence before back: {}", sequence);
        sequence = sequence.substring(0, sequence.length() - 1); // Remove last two characters
//            logger.info("Updated sequence after back: {}", sequence);
        logger.info("Current sequence after back : {}", sequence);

//        if (sequence.isEmpty()) {
//            return reset();
//        }

        gameResultResponse.setMessage("Removed previous result!");
        gameResultResponse.setSequence(sequence);
        gameResultResponse.setRecommendedBet(WAIT);


        gameResultStatus.setHandCount(gameResultStatus.getHandCount() - 1);
        gameResultResponse.setGameStatus(gameResultStatus);

        return provideGameResponse(gameResultResponse);
    }


    @PostMapping("/reset")
    public GameResultResponse reset() {
        logger.info("Resetting game state to initial values.");
        // Reset all variables

        GameResultResponse gameResultResponse = getGameResponse();

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();
        GameStatus gameStatus = gameStatusService.findByUserUuid(userUuid).orElse(null);
//        com.baccarat.markovchain.module.data.GameResponse gameResponse = gameResponseService.getGameResponseByUserUuid(userUuid);


        gameStatus.setHandCount(0);
        gameStatus.setWins(0);
        gameStatus.setLosses(0);
        gameStatus.setProfit(0);

        gameStatus.setPlayingUnits(100);
        gameStatusService.updateGameStatus(gameStatus.getGameStatusId(), gameStatus);

        gameResultResponse.setSequence("");
        gameResultResponse.setMessage("Game has been reset!");
        gameResultResponse.setBaseBetUnit(0);
        gameResultResponse.setInitialPlayingUnits(100);
        gameResultResponse.setSuggestedBetUnit(1);
        gameResultResponse.setRecommendedBet(WAIT);


        GameResultStatus gameResultStatus = new GameResultStatus(gameStatus.getHandCount(), gameStatus.getWins(), gameStatus.getLosses(), gameStatus.getProfit(), gameStatus.getPlayingUnits());
        gameResultResponse.setGameStatus(gameResultStatus);


//        initialize();


        logger.info(userPrincipal.getUsername() + ": Game state reset!");

        return provideGameResponse(gameResultResponse);
    }


    private Pair<Character, Double> combinePredictions(Pair<Character, Double> markovResult, String patternResult) {
        if (markovResult == null && patternResult == null) return new Pair<>(null, 0.0);
        if (markovResult == null) return new Pair<>(patternResult.charAt(0), 0.6);
        if (patternResult == null) return markovResult;

        char markovPrediction = markovResult.first, patternPrediction = patternResult.charAt(0);
        double markovConfidence = markovResult.second;

        return (markovPrediction == patternPrediction) ? new Pair<>(markovPrediction, Math.min(markovConfidence + 0.1, 1.0)) : (markovConfidence >= 0.5 ? markovResult : new Pair<>(patternPrediction, 0.6));
    }

    private GameResultResponse getGameResponse() {
        // Retrieve the current authenticated user
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        // Get the GameResponse for the user
        com.baccarat.markovchain.module.data.GameResponse gameResponse = gameResponseService.getGameResponseByUserUuid(userPrincipal.getUserUuid());

        // Create the GameResultResponse and populate it with default values if gameResponse is null
        GameResultResponse response = new GameResultResponse();
        if (gameResponse != null) {
            response.setBaseBetUnit(gameResponse.getBaseBetUnit());
            response.setSuggestedBetUnit(gameResponse.getSuggestedBetUnit());
            response.setInitialPlayingUnits(gameResponse.getInitialPlayingUnits());
            response.setRecommendedBet(gameResponse.getRecommendedBet());
            response.setSequence(gameResponse.getSequence().replace("1111", ""));
            response.setMessage(gameResponse.getMessage());
        } else {
            response.setBaseBetUnit(1);
            response.setSuggestedBetUnit(0);
            response.setInitialPlayingUnits(100);
            response.setRecommendedBet(WAIT);
            response.setSequence("");
            response.setMessage("");
        }

        // Get the GameStatus for the user
        GameStatus gameStatus = gameStatusService.findByUserUuid(userPrincipal.getUserUuid())
                .orElse(null);
        GameResultStatus gameResultStatus;

        if (gameStatus != null) {
            gameResultStatus = new GameResultStatus(gameStatus.getHandCount(), gameStatus.getWins(), gameStatus.getLosses(), gameStatus.getProfit(), gameStatus.getPlayingUnits());

        } else {
            gameResultStatus = new GameResultStatus(0, 0, 0, 0, 100);
        }

        response.setGameStatus(gameResultStatus);

        // Log the response
        logger.info("{}: Game Response-> {}", userPrincipal.getUsername(), response);

        // Return the populated GameResultResponse
        return response;
    }


    public GameResultResponse provideGameResponse(GameResultResponse gameResultResponse) {


        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        GameStatus gameStatus = gameStatusService.findByUserUuid(userPrincipal.getUserUuid())
                .orElseThrow(() -> new RuntimeException("No game status found for user: " + userPrincipal.getUserUuid()));


        gameStatus.setHandCount(gameResultResponse.getGameStatus().getHandCount());
        gameStatus.setWins(gameResultResponse.getGameStatus().getWins());
        gameStatus.setLosses(gameResultResponse.getGameStatus().getLosses());
        gameStatus.setProfit(gameResultResponse.getGameStatus().getProfit());
        gameStatus.setPlayingUnits(gameResultResponse.getGameStatus().getPlayingUnits());
        gameStatus.setDateLastUpdated(LocalDateTime.now());


        GameResponse gameResponse = gameResponseService.getGameResponseByUserUuid(userPrincipal.getUserUuid());

        // Update the game response fields
        gameResponse.setBaseBetUnit(gameResultResponse.getBaseBetUnit());
        gameResponse.setSuggestedBetUnit(gameResultResponse.getSuggestedBetUnit());
        gameResponse.setInitialPlayingUnits(gameResultResponse.getInitialPlayingUnits());
        gameResponse.setRecommendedBet(gameResultResponse.getRecommendedBet());
        gameResponse.setSequence(gameResultResponse.getSequence());
        gameResponse.setMessage(gameResultResponse.getMessage());
        gameResponse.setDateLastUpdated(LocalDateTime.now());

        // Persist the updated game response
        gameResponseService.createOrUpdateGameResponse(gameResponse);

        return gameResultResponse;
    }


}
