package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.common.concrete.RiskLevel;
import com.baccarat.markovchain.module.common.concrete.UserConfig;
import com.baccarat.markovchain.module.data.*;
import com.baccarat.markovchain.module.data.response.GameResultResponse;
import com.baccarat.markovchain.module.data.response.GameResultStatus;
import com.baccarat.markovchain.module.model.Pair;
import com.baccarat.markovchain.module.model.UserPrincipal;
import com.baccarat.markovchain.module.services.TrailingStopService;
import com.baccarat.markovchain.module.services.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/baccarat")
public class BaccaratController {

    private static final String STOP_PROFIT_REACHED = "Stop profit reached! Restart the game.";
    private static final String STOP_LOSS_REACHED = "Stop loss reached! Restart the game.";
    private static final String DAILY_LIMIT_REACHED = "Daily limit! Please play again after ";
    private static final String PREDICTION_CONFIDENCE_LOW = "Prediction confidence too low, no bet suggested.";
    private static final String TRAILING_STOP_TRIGGERED_LABEL = "Trailing stop triggered! Restart the game.";
    private static final String PLACE_YOUR_BET = "Place your bet";
    private static final int ZERO = 0;


    private static final Logger logger = LoggerFactory.getLogger(BaccaratController.class);

    private static final double STOP_PROFIT_PERCENTAGE = 0.30;
    private static final double STOP_LOSS_PERCENTAGE = 0.10;
    private static final int MAX_HANDS = 60;

    private final MarkovChain markovChain;
    private final PatternRecognizer patternRecognizer;
    private final JournalServiceImpl journalService;
    private final GameStatusService gameStatusService;
    private final GameResponseService gameResponseService;
    private final TrailingStopService trailingStopService;
    private final GamesArchiveService gamesArchiveService;

    private final UserConfigService configService;
    private final String WAIT = "Wait..";


    @Autowired
    public BaccaratController(MarkovChain markovChain, PatternRecognizer patternRecognizer, JournalServiceImpl journalService, GameStatusService gameStatusService, GameResponseService gameResponseService, TrailingStopService trailingStopService, GamesArchiveService gamesArchiveService, UserConfigService configService) {
        this.markovChain = markovChain;
        this.patternRecognizer = patternRecognizer;
        this.journalService = journalService;
        this.gameStatusService = gameStatusService;
        this.gameResponseService = gameResponseService;
        this.trailingStopService = trailingStopService;
        this.gamesArchiveService = gamesArchiveService;
        this.configService = configService;

    }


    @PostMapping("/play")
    public GameResultResponse play(@RequestParam String userInput,
                                   @RequestParam String recommendedBet,
                                   @RequestParam String riskLevel,
                                   @RequestParam int suggestedUnit) {

        return processGame(userInput, recommendedBet,riskLevel, suggestedUnit);
    }


    public GameResultResponse processGame(String userInput, String recommendedBet,String riskLevel, int suggestedUnit) {


        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userName = userPrincipal.getUsername();

        GameResultResponse existingGame = getGameResponse(userPrincipal);

        existingGame.setRiskLevel(riskLevel);

        logger.info(userName + ": Received user input: {}", userInput);
        logger.info(userName + ": Received recommendedBet input: {}", recommendedBet);
        logger.info(userName + ": Received suggestedUnit input: {}", suggestedUnit);

        if (hasReachedDailyJournalLimit()) {


            // Get the current time in UTC
            ZonedDateTime nowUTC = ZonedDateTime.now(ZoneOffset.UTC);

            // Define another time in UTC (e.g., 2 hours ago)
            ZonedDateTime pastUTC = nowUTC.minusHours(4);

            // Calculate the difference between the two times
            Duration duration = Duration.between(pastUTC, nowUTC);

            // Get the difference in various units
            long hours = duration.toHours();
            long minutes = duration.toMinutes();
            long seconds = duration.getSeconds();

            String diff;
            if(hours>1){
                diff = hours+" hours";
            }else{
                diff = minutes+" minutes";
            }

            System.out.println("Time difference: " + hours + " hours, " + minutes + " minutes");


            existingGame.setDailyLimitReached(true);
            existingGame.setMessage(DAILY_LIMIT_REACHED+ diff);
            existingGame.setRecommendedBet(recommendedBet);
            logger.info(userName + ": "+existingGame.getMessage());
            return existingGame;
        }

        if (existingGame.getMessage().equals(TRAILING_STOP_TRIGGERED_LABEL)) {
            logger.info(userName + ": Trailing stop triggered, skipping game");
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


    public void initialize(GameResultResponse game) {


        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        GameResponse gameResponse;

        gameResponse = new com.baccarat.markovchain.module.data.GameResponse();
        gameResponse.setUserUuid(userUuid);
        gameResponse.setBaseBetUnit(game.getBaseBetUnit());
        gameResponse.setSuggestedBetUnit(game.getSuggestedBetUnit());
        gameResponse.setInitialPlayingUnits(game.getInitialPlayingUnits());
        gameResponse.setLossCounter(game.getLossCounter());
        gameResponse.setRecommendedBet(game.getRecommendedBet());
        gameResponse.setSequence(game.getSequence());
        gameResponse.setMessage("Initialized");
        gameResponse.setRiskLevel(game.getRiskLevel());
        gameResponse.setDateLastUpdated(LocalDateTime.now());
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

        String winLose = response.getGameStatus().getWins() + "/" + response.getGameStatus().getLosses();
        Journal savedJournal = journalService.saveJournal(new Journal(ZERO, userUuid, winLose, response.getGameStatus().getHandCount(),
                response.getGameStatus().getProfit()));

        if (savedJournal != null) {

            GameResultStatus gameResultStatus = response.getGameStatus();

            gamesArchiveService.addGameArchive(new GamesArchive(savedJournal.getJournalId(), response.getBaseBetUnit(),
                    response.getSuggestedBetUnit(), response.getLossCounter(), response.getRecommendedBet(),
                    response.getSequence(), response.getHandResult(), "Archived", gameResultStatus.getHandCount(),
                    gameResultStatus.getWins(), gameResultStatus.getLosses(), gameResultStatus.getProfit(),
                    gameResultStatus.getPlayingUnits(),response.getRiskLevel()));
        }


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

        ZonedDateTime nowUTC = ZonedDateTime.now(ZoneOffset.UTC);

        // Extract just the date portion
        LocalDate currentUTCDate = nowUTC.toLocalDate();




        List<Journal> journals = journalService.getJournalsByUserUuidAndDateCreated(userPrincipal.getUserUuid(),currentUTCDate);
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
        int profit = gameResultResponse.getGameStatus().getProfit();

        if (profit <= -STOP_LOSS_PERCENTAGE * gameResultResponse.getInitialPlayingUnits()) {

            return true;
        }
        return false;
    }


    public int calculateWager(double confidence, GameResultResponse gameResultResponse) {

//        if(gameResultResponse.getRiskLevel() == null){
//            gameResultResponse.setRiskLevel(RiskLevel.LOW.getValue());
//        }

        if (gameResultResponse.getRiskLevel().equals(RiskLevel.LOW.getValue())) {
            if (confidence < 0.6) {
                return 1;
            } else if (confidence < 0.8) {
                return 2;
            } else {
                return 3;  // High confidence, maximum bet
            }
        } else {
            return 1; //base unit
        }

    }

    private GameResultResponse handleBet(GameResultResponse gameResultResponse, UserPrincipal userPrincipal,
                                         String userInput, Pair<Character, Double> combinedPrediction,
                                         String predictedBet, int suggestedUnit) {


        String username = userPrincipal.getUsername();

//        int betSize = (int) Math.ceil(1 * combinedPrediction.second * 5);

        int betSize = calculateWager(combinedPrediction.second, gameResultResponse);

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
            gameResultResponse.setMessage(PLACE_YOUR_BET);
            int lossCounter = gameResultResponse.getLossCounter();
            logger.info("LossCounter:" + lossCounter);
            gameResultResponse.setRecommendedBet(recommendedBet);

            return validateGameResult(suggestedUnit, betSize, predictedBet, userInput, username, gameResultResponse, recommendedBet);
        }

        return validateGameResult(suggestedUnit, betSize, predictedBet, userInput, username, gameResultResponse, recommendedBet);

    }

    private GameResultResponse validateGameResult(int suggestedUnit, int betSize, String predictedBet,
                                                  String userInput, String username,
                                                  GameResultResponse gameResultResponse, String nextPredictedBet) {


        if (!predictedBet.equals(WAIT)) {

            String previousPrediction = predictedBet.equals("Player") ? "p" : "b";


            if (previousPrediction.equals(userInput)) {

                boolean toValidate = false;
                if (gameResultResponse.getLossCounter() >= 2) {
                    logger.info(username + ": Virtual won!");
                    gameResultResponse.setMessage(PLACE_YOUR_BET);
                    gameResultResponse.setLossCounter(0);
                } else {
                    toValidate = true;
                    logger.info(username + ": You won!");
                    gameResultResponse.setMessage("You won!");

                }

                return settleLimitAndValidation(updateProfitAndFund(toValidate, gameResultResponse, suggestedUnit, betSize, true), nextPredictedBet);
            } else {

                if (gameResultResponse.getLossCounter() >= 2) {
                    logger.info(username + ": Virtual lost!");
                    gameResultResponse.setMessage("Virtual lost!");

                } else {

                    logger.info(username + ": You lost!");
                    gameResultResponse.setMessage("You lost!");


                }


                return settleLimitAndValidation(updateProfitAndFund(true, gameResultResponse, suggestedUnit, betSize, false), nextPredictedBet);
            }
        } else {
            gameResultResponse.setSuggestedBetUnit(0);

            return settleLimitAndValidation(updateProfitAndFund(false, gameResultResponse, suggestedUnit, betSize, true), nextPredictedBet);
        }


    }

    private GameResultResponse settleLimitAndValidation(GameResultResponse gameResultResponse, String nextPredictedBet) {


        int profit = gameResultResponse.getGameStatus().getProfit();
        int playingUnit = gameResultResponse.getGameStatus().getPlayingUnits();


        gameResultResponse.setSequence(gameResultResponse.getSequence());

        gameResultResponse.setRecommendedBet(WAIT);
        gameResultResponse.setDailyLimitReached(hasReachedDailyJournalLimit());

        if (hasReachedStopProfit(gameResultResponse)) {
            logger.warn(": Reached stop profit. New profit: {}, New playing fund: {}", profit, playingUnit);
            gameResultResponse.setMessage(STOP_PROFIT_REACHED);
            gameResultResponse.setSuggestedBetUnit(0);
            gameResultResponse.setRecommendedBet(WAIT);
//            return saveAndReturn(provideGameResponse(gameResultResponse));
            return provideGameResponse(gameResultResponse);
        } else if (hasReachedStopLoss(gameResultResponse)) {
            logger.warn(": Reached stop loss. New profit: {}, New playing fund: {}", profit, playingUnit);
            gameResultResponse.setMessage(STOP_LOSS_REACHED);
            gameResultResponse.setSuggestedBetUnit(0);
            gameResultResponse.setRecommendedBet(WAIT);
//            return saveAndReturn(provideGameResponse(gameResultResponse));
            return provideGameResponse(gameResultResponse);
        } else {

            gameResultResponse.setRecommendedBet(nextPredictedBet);

            if (gameResultResponse.getLossCounter() >= 2
                    && !gameResultResponse.getMessage().equals("Virtual won!")) {
                gameResultResponse.setSuggestedBetUnit(0);
                gameResultResponse.setMessage("Wait for virtual win.");
            }

            //evaluate trailing stop
            GameResultResponse gameResultResponseWithTrailingStop = trailingStop(gameResultResponse, false);
            if (gameResultResponseWithTrailingStop.getMessage().equals(TRAILING_STOP_TRIGGERED_LABEL)) {
                return provideGameResponse(gameResultResponseWithTrailingStop);
            }
            // code below will not be executed if the above condition is true
            gameResultResponse.setTrailingStop(gameResultResponseWithTrailingStop.getTrailingStop());
            if (gameResultResponse.getSuggestedBetUnit() <= 0) {
                gameResultResponse.setSuggestedBetUnit(1);
            }
            return provideGameResponse(gameResultResponse);
        }

    }

    private GameResultResponse trailingStop(GameResultResponse gameResultResponse, boolean isUndo) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int profit = gameResultResponse.getGameStatus().getProfit();
        if (gameResultResponse.getGameStatus().getProfit() >= 10) {

            int currentProfit = gameResultResponse.getGameStatus().getProfit();
            double trailingPercent = 5;
            double stopProfit = currentProfit - trailingPercent;

            TrailingStop existingTrailingStop = trailingStopService.getTrailingStopByUserUuid(userPrincipal.getUserUuid());

            if (existingTrailingStop == null) {
                trailingStopService.saveOrUpdate(new TrailingStop(userPrincipal.getUserUuid(), currentProfit
                        , trailingPercent, stopProfit, currentProfit));
            } else {

                if (!isUndo) {
                    if (profit > existingTrailingStop.getHighestProfit()) {
                        existingTrailingStop.setHighestProfit(profit);
                        existingTrailingStop.setStopProfit(stopProfit);
                        trailingStopService.saveOrUpdate(existingTrailingStop);
                    }
                } else {
                    existingTrailingStop.setHighestProfit(profit);
                    existingTrailingStop.setStopProfit(stopProfit);
                    trailingStopService.saveOrUpdate(existingTrailingStop);
                }
            }

        } else {
            if (isUndo) {
                trailingStopService.deleteTrailingStopByUserUuid(userPrincipal.getUserUuid());
            }
        }

        String trailingStop = evaluateTrailingStop(userPrincipal.getUserUuid(), profit);
        if (trailingStop.equals(TRAILING_STOP_TRIGGERED_LABEL)) {
            gameResultResponse.setRecommendedBet(WAIT);
            gameResultResponse.setSuggestedBetUnit(0);
            gameResultResponse.setMessage(trailingStop);
        } else {
            gameResultResponse.setTrailingStop(trailingStop);
        }
        gameResultResponse.setSequence(gameResultResponse.getSequence().replace("1111", ""));
//        gameResultResponse.setHandResult(gameResultResponse.getHandResult());

        return gameResultResponse;
    }

    public String evaluateTrailingStop(String userUuid, int currentProfit) {


        TrailingStop trailingStop = trailingStopService.getTrailingStopByUserUuid(userUuid);

        if (trailingStop == null) {
            return "";
        }

        double stopProfit = trailingStop.getStopProfit();
        int highestProfit = trailingStop.getHighestProfit();
        double trailingPercent = trailingStop.getTrailingPercent();


        if (currentProfit > highestProfit) {
            highestProfit = currentProfit;
            stopProfit = highestProfit * (1 - trailingPercent); // Recalculate the stop price

            trailingStopService.saveOrUpdate(new TrailingStop(trailingStop.getTrailingStopId(), userUuid,
                    trailingStop.getInitial(), trailingStop.getTrailingPercent(), stopProfit, highestProfit));
        }

        // Check if the stop price has been triggered
        if (currentProfit <= stopProfit) {
            return TRAILING_STOP_TRIGGERED_LABEL;
        } else {
            System.out.println("highestProfit: " + highestProfit);
            System.out.println("Current Profit: " + currentProfit + ", Stop Profit: " + stopProfit);
            return stopProfit + "";
        }
    }


    private GameResultResponse updateProfitAndFund(boolean isToValidate, GameResultResponse gameResultResponse, int suggestedUnit, int betSize, boolean isWin) {


        int profit = gameResultResponse.getGameStatus().getProfit();
        int playingUnit = gameResultResponse.getGameStatus().getPlayingUnits();
        int totalWins = gameResultResponse.getGameStatus().getWins();
        int totalLosses = gameResultResponse.getGameStatus().getLosses();
        int currentLossCount = gameResultResponse.getLossCounter();

        if (isToValidate) {

            if (gameResultResponse.getSuggestedBetUnit() != 0) {


                if (isWin) {
                    profit += suggestedUnit;
                    playingUnit += suggestedUnit;
                    totalWins++;
                    currentLossCount = 0;
                    gameResultResponse.setHandResult(gameResultResponse.getHandResult() + "W");
                } else {
                    currentLossCount = currentLossCount + 1;
                    profit -= suggestedUnit;
                    playingUnit -= suggestedUnit;
                    totalLosses++;
                    betSize -= 2;
                    betSize = betSize <= 0 ? 1 : betSize;
                    gameResultResponse.setHandResult(gameResultResponse.getHandResult() + "L");

                }
            }


        } else {
            int lossCount = gameResultResponse.getLossCounter();
            if (lossCount > 0) {

                betSize -= 2;
                betSize = betSize == 0 ? 1 : betSize;
            }

            gameResultResponse.setHandResult(gameResultResponse.getHandResult());
        }


        GameResultStatus gameResultStatus = gameResultResponse.getGameStatus();
        gameResultStatus.setProfit(profit);
        gameResultStatus.setPlayingUnits(playingUnit);
        gameResultStatus.setWins(totalWins);
        gameResultStatus.setLosses(totalLosses);
        gameResultResponse.setLossCounter(currentLossCount);
        gameResultResponse.setGameStatus(gameResultStatus);

        int temptBetSize = gameResultStatus.getProfit() - betSize;
        if (temptBetSize < -10) {
            betSize = Math.abs(temptBetSize + 10);
        }


        gameResultResponse.setSuggestedBetUnit(betSize);

        return gameResultResponse;
    }

    @GetMapping("/current-state")
    public GameResultResponse getCurrentState(@RequestParam String message) {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info(userPrincipal.getUsername() + ": Fetching current game state. " + message);


        return getGameResponse(userPrincipal);
    }


    @PostMapping("/undo")
    public GameResultResponse back() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        GameResultResponse existingGame = getGameResponse(userPrincipal);

        if (hasReachedDailyJournalLimit()) {
            existingGame.setDailyLimitReached(true);
            existingGame.setMessage(DAILY_LIMIT_REACHED);
            existingGame.setRecommendedBet(WAIT);
            return existingGame;
        }

        if (existingGame.getMessage().equals(TRAILING_STOP_TRIGGERED_LABEL)) {
            return existingGame;
        }


        Optional<GameResponse> g2 = gameResponseService.findFirstByUserUuidOrderByGameResponseIdDesc(userPrincipal.getUserUuid());
        GameResponse gameResponseToDelete = g2.get();

        gameResponseService.deleteGameResponse(gameResponseToDelete.getGameResponseId());

        GameStatus gameStatus = gameStatusService.findByGameResponseId(gameResponseToDelete.getGameResponseId()).get();
        gameStatusService.deleteById(gameStatus.getGameStatusId());

        GameResultResponse newGameResponse = getGameResponse(userPrincipal);
        trailingStop(newGameResponse, true);

        return newGameResponse;

    }


    @PostMapping("/reset")
    public GameResultResponse reset(@RequestParam String yesNo) {
        logger.info("Resetting game state to initial values.");

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        try {

            if (yesNo.equalsIgnoreCase("yes")) {
                saveAndReturn(getGameResponse(userPrincipal));
            }

            deleteGamesByUserUuid(userUuid);
            trailingStopService.deleteTrailingStopByUserUuid(userUuid);

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
        gameResultResponse.setLossCounter(0);
        gameResultResponse.setRecommendedBet(WAIT);
        gameResultResponse.setRiskLevel(RiskLevel.LOW.getValue());
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
            reset("no");
        }

        Optional<GameResponse> g2 = gameResponseService.findFirstByUserUuidOrderByGameResponseIdDesc(userPrincipal.getUserUuid());
        GameResponse latesGameResponse = g2.get();


        // Create the GameResultResponse and populate it with default values if gameResponse is null
        GameResultResponse response = new GameResultResponse();

        response.setBaseBetUnit(latesGameResponse.getBaseBetUnit());
        response.setSuggestedBetUnit(latesGameResponse.getSuggestedBetUnit());
        response.setInitialPlayingUnits(latesGameResponse.getInitialPlayingUnits());
        response.setLossCounter(latesGameResponse.getLossCounter());
        response.setRecommendedBet(latesGameResponse.getRecommendedBet());
        response.setSequence(latesGameResponse.getSequence());
        response.setHandResult(latesGameResponse.getHandResult());
        response.setMessage(latesGameResponse.getMessage());
        response.setRiskLevel(latesGameResponse.getRiskLevel());

        // Get the GameStatus for the user
        GameStatus gameStatus = gameStatusService.findByGameResponseId(latesGameResponse.getGameResponseId()).orElse(null);
        GameResultStatus gameResultStatus;

        if (gameStatus != null) {
            gameResultStatus = new GameResultStatus(gameStatus.getHandCount(), gameStatus.getWins(), gameStatus.getLosses(), gameStatus.getProfit(), gameStatus.getPlayingUnits());

        } else {
            gameResultStatus = new GameResultStatus(0, 0, 0, 0, 100);
        }

        response.setGameStatus(gameResultStatus);


        TrailingStop trailingStop = trailingStopService.getTrailingStopByUserUuid(userPrincipal.getUserUuid());
        if (trailingStop != null) {
            response.setTrailingStop(String.valueOf(trailingStop.getStopProfit()));
        } else {
            response.setTrailingStop("0");
        }

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
        gameResponse.setLossCounter(gameResultResponse.getLossCounter());
        gameResponse.setInitialPlayingUnits(gameResultResponse.getInitialPlayingUnits());
        gameResponse.setRecommendedBet(gameResultResponse.getRecommendedBet());
        gameResponse.setSequence(gameResultResponse.getSequence());
        gameResponse.setHandResult(gameResultResponse.getHandResult() == null ? "" : gameResultResponse.getHandResult());
        gameResponse.setMessage(gameResultResponse.getMessage());
        gameResponse.setRiskLevel(gameResultResponse.getRiskLevel());
//        gameResponse.setDateLastUpdated(LocalDateTime.now());
        // Persist the updated game response
        GameResponse newOneGameResponse = gameResponseService.createOrUpdateGameResponse(gameResponse);


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
