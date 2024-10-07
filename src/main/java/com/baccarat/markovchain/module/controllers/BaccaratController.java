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
    private static final String PREDICTION_CONFIDENCE_LOW = "Prediction confidence too low, no bet suggested.";
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


    @PostMapping("/play")
    public GameResultResponse play(@RequestParam String userInput, @RequestParam String recommendedBet, @RequestParam int suggestedUnit) {

        return processGame(userInput, recommendedBet, suggestedUnit);
    }


    public GameResultResponse processGame(String userInput, String recommendedBet, int suggestedUnit) {


        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userName = userPrincipal.getUsername();

        GameResultResponse existingGame = getGameResponse(userPrincipal);

        logger.info(userName + ": Received user input: {}", userInput);
        logger.info(userName + ": Received recommendedBet input: {}", recommendedBet);
        logger.info(userName + ": Received suggestedUnit input: {}", suggestedUnit);

        if (hasReachedDailyJournalLimit()) {
            existingGame.setDailyLimitReached(true);
            existingGame.setMessage(DAILY_LIMIT_REACHED);
            existingGame.setRecommendedBet(recommendedBet);
            return existingGame;
        }

        GameResultResponse gameResultResponse = updateSequenceAndUpdateHandCount(existingGame, userInput);


        // Generate predictions using Markov chain and pattern recognition
        String sequence = gameResultResponse.getSequence();
        markovChain.train(sequence);
        Pair<Character, Double> markovPrediction = markovChain.predictNext(sequence.charAt(sequence.length() - 1));
        String patternPrediction = patternRecognizer.findPattern(sequence);

        logger.info(userPrincipal.getUsername() + ":Pattern Prediction: {}", patternPrediction);

        // Combine predictions and handle the bet
        Pair<Character, Double> combinedPrediction = combinePredictions(markovPrediction, patternPrediction);

        return handleBet(gameResultResponse, userPrincipal, userInput, combinedPrediction, recommendedBet, suggestedUnit);
    }


    //        @PostMapping("/init-config")
    public void initialize(GameResultResponse game) {


        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        GameResponse gameResponse;

        gameResponse = new com.baccarat.markovchain.module.data.GameResponse();
        gameResponse.setUserUuid(userUuid);
        gameResponse.setBaseBetUnit(game.getBaseBetUnit());
        gameResponse.setSuggestedBetUnit(game.getSuggestedBetUnit());
        gameResponse.setInitialPlayingUnits(game.getInitialPlayingUnits());
        gameResponse.setRecommendedBet(game.getRecommendedBet());
        gameResponse.setSequence(game.getSequence());
        gameResponse.setMessage("Initialized");
        gameResponseService.createOrUpdateGameResponse(gameResponse);

        GameResponse g = gameResponseService.getGameResponseByUserUuid(userUuid);

        GameStatus s = new GameStatus();
        s.setGameResponseId(g.getGameResponseId());
        s.setHandCount(game.getGameStatus().getHandCount());
        s.setWins(game.getGameStatus().getWins());
        s.setLosses(game.getGameStatus().getLosses());
        s.setProfit(game.getGameStatus().getProfit());
        s.setPlayingUnits(game.getGameStatus().getPlayingUnits());
        s.setDateLastUpdated(LocalDateTime.now());
        gameStatusService.save(s);


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


    private GameResultResponse updateSequenceAndUpdateHandCount(GameResultResponse existingGame, String userInput) {

        // Fetch the current game response once

        GameResultStatus gameStatus = existingGame.getGameStatus();
        // Update the sequence and hand count
        String sequence = existingGame.getSequence() == null ? "1111" : existingGame.getSequence() + userInput;
        int handCount = gameStatus == null ? 1 : gameStatus.getHandCount() + 1;

        assert gameStatus != null;

        gameStatus.setHandCount(handCount);
        existingGame.setGameStatus(gameStatus);
        existingGame.setSequence(sequence);

        return existingGame;
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
        return handCount >= MAX_HANDS;
    }


    private GameResultResponse handleBet(GameResultResponse gameResultResponse, UserPrincipal userPrincipal, String userInput, Pair<Character, Double> combinedPrediction, String predictedBet, int suggestedUnit) {


        String username = userPrincipal.getUsername();

        int betSize = (int) Math.ceil(1 * combinedPrediction.second * 5);

        if (combinedPrediction.first == null || combinedPrediction.second < 0.6) {


            logger.info(username + ": " + PREDICTION_CONFIDENCE_LOW);
            gameResultResponse.setMessage(PREDICTION_CONFIDENCE_LOW);

            gameResultResponse.setSequence(gameResultResponse.getSequence());
            gameResultResponse.setRecommendedBet(WAIT);
            return validateGameResult(suggestedUnit, betSize, predictedBet, userInput, username, gameResultResponse, gameResultResponse.getRecommendedBet());

        }


        String prediction = String.valueOf(combinedPrediction.first);
        String recommendedBet = Objects.equals(prediction, "p") ? "Player" : "Banker";
        return resolveBet(gameResultResponse, userPrincipal, userInput, betSize, suggestedUnit, recommendedBet, predictedBet);
    }

    private GameResultResponse resolveBet(GameResultResponse gameResultResponse, UserPrincipal userPrincipal, String userInput, int betSize, int suggestedUnit, String recommendedBet, String predictedBet) {

        String username = userPrincipal.getUsername();

        gameResultResponse.setDailyLimitReached(hasReachedDailyJournalLimit());

        if (predictedBet.equals(WAIT) || predictedBet.isEmpty()) {
            gameResultResponse.setSuggestedBetUnit(suggestedUnit);
            gameResultResponse.setMessage("Place your bet");
            gameResultResponse.setRecommendedBet(recommendedBet);
//            return provideGameResponse(gameResultResponse);
            return validateGameResult(suggestedUnit, betSize, predictedBet, userInput, username, gameResultResponse, gameResultResponse.getRecommendedBet());
        }

        return validateGameResult(suggestedUnit, betSize, predictedBet, userInput, username, gameResultResponse, recommendedBet);

    }

    private GameResultResponse validateGameResult(int suggestedUnit, int betSize, String predictedBet, String userInput, String username, GameResultResponse gameResultResponse, String nextPredictedBet) {


        if (!predictedBet.equals(WAIT)) {

            String previousPrediction = predictedBet.equals("Player") ? "p" : "b";
            if (previousPrediction.equals(userInput)) {
                logger.info(username + ": You won!");
                gameResultResponse.setMessage("You won!");
                return settleLimitAndValidation(updateProfitAndFund(true, gameResultResponse, suggestedUnit, betSize, true), nextPredictedBet, true);
            } else {
                logger.info(username + ": You lost!");
                gameResultResponse.setMessage("You lost!");
                return settleLimitAndValidation(updateProfitAndFund(true, gameResultResponse, suggestedUnit, betSize, false), nextPredictedBet, false);
            }
        } else {
            return settleLimitAndValidation(updateProfitAndFund(false, gameResultResponse, suggestedUnit, betSize, true), nextPredictedBet, true);
        }


    }

    private GameResultResponse settleLimitAndValidation(GameResultResponse gameResultResponse, String nextPredictedBet, boolean isWon) {


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
//            gameResultResponse.setMessage(isWon ? "You won!" : "You lost!");
            gameResultResponse.setRecommendedBet(nextPredictedBet);
            return provideGameResponse(gameResultResponse);
        }

    }

    private GameResultResponse updateProfitAndFund(boolean isToValidate, GameResultResponse gameResultResponse, int suggestedUnit, int betSize, boolean isWin) {

        int profit = gameResultResponse.getGameStatus().getProfit();
        int playingUnit = gameResultResponse.getGameStatus().getPlayingUnits();
        int totalWins = gameResultResponse.getGameStatus().getWins();
        int totalLosses = gameResultResponse.getGameStatus().getLosses();

        if (isToValidate) {
            if (isWin) {
                profit += suggestedUnit;
                playingUnit += suggestedUnit;
                totalWins++;
            } else {
                profit -= suggestedUnit;
                playingUnit -= suggestedUnit;
                totalLosses++;
            }
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


        return getGameResponse(userPrincipal);
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


    @PostMapping("/undo")
    public GameResultResponse back() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Optional<GameResponse> g2 = gameResponseService.findFirstByUserUuidOrderByGameResponseIdDesc(userPrincipal.getUserUuid());
        GameResponse gameResponseToDelete = g2.get();

        gameResponseService.deleteGameResponse(gameResponseToDelete.getGameResponseId());

        GameStatus gameStatus = gameStatusService.findByGameResponseId(gameResponseToDelete.getGameResponseId()).get();
        gameStatusService.deleteById(gameStatus.getGameStatusId());


        return getGameResponse(userPrincipal);

    }


    @PostMapping("/reset")
    public GameResultResponse reset() {
        logger.info("Resetting game state to initial values.");

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        try {
            deleteGamesByUserUuid(userUuid);

            GameStatus gameStatus = createInitialGameStatus();
            GameResultResponse gameResultResponse = createInitialGameResultResponse(gameStatus);

            initialize(gameResultResponse);

            logger.info(userPrincipal.getUsername() + ": Game state reset!");
            return gameResultResponse;
        } catch (Exception e) {
            logger.error("Error resetting game state", e);
            throw new RuntimeException("Failed to reset game state", e);
        }
    }

    private void deleteGamesByUserUuid(String userUuid) {
        List<GameResponse> games = gameResponseService.findAllByUserUuid(userUuid);
        for (GameResponse game : games) {
            gameResponseService.deleteGameResponse(game.getGameResponseId());
            gameStatusService.deleteById(gameStatusService.findByGameResponseId(game.getGameResponseId()).get().getGameStatusId());
        }
    }

    private GameStatus createInitialGameStatus() {
        GameStatus gameStatus = new GameStatus();
        gameStatus.setHandCount(0);
        gameStatus.setWins(0);
        gameStatus.setLosses(0);
        gameStatus.setProfit(0);
        gameStatus.setPlayingUnits(100);
        return gameStatus;
    }

    private GameResultResponse createInitialGameResultResponse(GameStatus gameStatus) {
        GameResultResponse gameResultResponse = new GameResultResponse();
        gameResultResponse.setSequence("1111");
        gameResultResponse.setMessage("Game reset!");
        gameResultResponse.setBaseBetUnit(1);
        gameResultResponse.setInitialPlayingUnits(100);
        gameResultResponse.setSuggestedBetUnit(1);
        gameResultResponse.setRecommendedBet(WAIT);

        GameResultStatus gameResultStatus = new GameResultStatus(
                gameStatus.getHandCount(),
                gameStatus.getWins(),
                gameStatus.getLosses(),
                gameStatus.getProfit(),
                gameStatus.getPlayingUnits()
        );
        gameResultResponse.setGameStatus(gameResultStatus);

        return gameResultResponse;
    }


    private Pair<Character, Double> combinePredictions(Pair<Character, Double> markovResult, String patternResult) {
        if (markovResult == null && patternResult == null) return new Pair<>(null, 0.0);
        if (markovResult == null) return new Pair<>(patternResult.charAt(0), 0.6);
        if (patternResult == null) return markovResult;

        char markovPrediction = markovResult.first, patternPrediction = patternResult.charAt(0);
        double markovConfidence = markovResult.second;

        return (markovPrediction == patternPrediction) ? new Pair<>(markovPrediction, Math.min(markovConfidence + 0.1, 1.0)) : (markovConfidence >= 0.5 ? markovResult : new Pair<>(patternPrediction, 0.6));
    }

    private GameResultResponse getGameResponse(UserPrincipal userPrincipal) {

        // Get the GameResponse for the user
        Optional<GameResponse> g1 = gameResponseService.findFirstByUserUuidOrderByGameResponseIdDesc(userPrincipal.getUserUuid());
        if (g1.isEmpty()) {
            reset();
        }

        Optional<GameResponse> g2 = gameResponseService.findFirstByUserUuidOrderByGameResponseIdDesc(userPrincipal.getUserUuid());
        GameResponse latesGameResponse = g2.get();


        // Create the GameResultResponse and populate it with default values if gameResponse is null
        GameResultResponse response = new GameResultResponse();

        response.setBaseBetUnit(latesGameResponse.getBaseBetUnit());
        response.setSuggestedBetUnit(latesGameResponse.getSuggestedBetUnit());
        response.setInitialPlayingUnits(latesGameResponse.getInitialPlayingUnits());
        response.setRecommendedBet(latesGameResponse.getRecommendedBet());
        response.setSequence(latesGameResponse.getSequence());
        response.setMessage(latesGameResponse.getMessage());


        // Get the GameStatus for the user
        GameStatus gameStatus = gameStatusService.findByGameResponseId(latesGameResponse.getGameResponseId()).orElse(null);
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
        GameResponse gameResponse = new GameResponse();// gameResponseService.getGameResponseByUserUuid(userPrincipal.getUserUuid());

        // Update the game response fields
        gameResponse.setUserUuid(userPrincipal.getUserUuid());
        gameResponse.setBaseBetUnit(gameResultResponse.getBaseBetUnit());
        gameResponse.setSuggestedBetUnit(gameResultResponse.getSuggestedBetUnit());
        gameResponse.setInitialPlayingUnits(gameResultResponse.getInitialPlayingUnits());
        gameResponse.setRecommendedBet(gameResultResponse.getRecommendedBet());
        gameResponse.setSequence(gameResultResponse.getSequence());
        gameResponse.setMessage(gameResultResponse.getMessage());
        gameResponse.setDateLastUpdated(LocalDateTime.now());
        // Persist the updated game response
        GameResponse newOneGameResponse = gameResponseService.createOrUpdateGameResponse(gameResponse);

//        Optional<GameResponse> g = gameResponseService.findFirstByUserUuidOrderByGameResponseIdDesc(userPrincipal.getUserUuid());
//        GameResponse latestGameResponse = g.get();

//        GameStatus gameStatus = gameStatusService.findByGameResponseId(latestGameResponse.getGameResponseId())
//                .orElseThrow(() -> new RuntimeException("No game status found for user: " + userPrincipal.getUserUuid()));


        GameResultStatus gameResultStatus = new GameResultStatus();

        gameResultStatus.setHandCount(gameResultResponse.getGameStatus().getHandCount());
        gameResultStatus.setWins(gameResultResponse.getGameStatus().getWins());
        gameResultStatus.setLosses(gameResultResponse.getGameStatus().getLosses());
        gameResultStatus.setProfit(gameResultResponse.getGameStatus().getProfit());
        gameResultStatus.setPlayingUnits(gameResultResponse.getGameStatus().getPlayingUnits());


        GameStatus gameStatus = new GameStatus();
        gameStatus.setGameResponseId(newOneGameResponse.getGameResponseId());
        gameStatus.setHandCount(gameResultResponse.getGameStatus().getHandCount());
        gameStatus.setWins(gameResultResponse.getGameStatus().getWins());
        gameStatus.setLosses(gameResultResponse.getGameStatus().getLosses());
        gameStatus.setProfit(gameResultResponse.getGameStatus().getProfit());
        gameStatus.setPlayingUnits(gameResultResponse.getGameStatus().getPlayingUnits());
        gameStatus.setDateLastUpdated(LocalDateTime.now());
        gameStatusService.save(gameStatus);

        gameResultResponse.setGameStatus(gameResultStatus);


        return gameResultResponse;
    }


}
