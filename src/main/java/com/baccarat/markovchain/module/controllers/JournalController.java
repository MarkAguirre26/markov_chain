package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.common.concrete.UserConfig;
import com.baccarat.markovchain.module.data.Config;
import com.baccarat.markovchain.module.data.Journal;
import com.baccarat.markovchain.module.model.UserPrincipal;
import com.baccarat.markovchain.module.response.JournalResponse;
import com.baccarat.markovchain.module.services.JournalService;
import com.baccarat.markovchain.module.services.impl.UserConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/journals")
public class JournalController {

    private static final Logger logger = LoggerFactory.getLogger(JournalController.class);
    private final JournalService journalService;
    private final UserConfigService configService;


    @Autowired
    public JournalController(JournalService journalService, UserConfigService configService) {
        this.journalService = journalService;
        this.configService = configService;
    }


    @GetMapping("/dateCreated")
    public ResponseEntity<List<JournalResponse>> getJournalsByDateCreated(
            @RequestParam("dateCreated") LocalDate effectiveFrom) {



        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        logger.info(userPrincipal.getUsername()+ ": Date created: {}", effectiveFrom);

//        logger.info("User UUID: {}", userUuid);

        List<Journal> journals;
        journals = journalService.getJournalsByUserUuidAndDateCreated(userUuid, effectiveFrom);


        int dailyLimit = getDailyLimit();

        List<JournalResponse> journalResponses;


        // Create journal responses with a sequential shoe number, limited by dailyLimit
        journalResponses = IntStream.range(0, Math.min(journals.size(), dailyLimit))
                .mapToObj(i -> new JournalResponse(i + 1,
                        journals.get(i).getHand(),
                        journals.get(i).getProfit(),
                        journals.get(i).getDateCreated()))
                .collect(Collectors.toList());

        // Fill in remaining responses if fewer than totalResponses
        int remainingShoe = journals.size() + 1;
        while (journalResponses.size() < dailyLimit) {
            journalResponses.add(new JournalResponse(remainingShoe++, 0, 0, LocalDate.now()));
        }

        logger.info(userPrincipal.getUsername()+ ": Journals fetched: {}", journals.size());
        return ResponseEntity.ok(journalResponses);
    }

    private int getDailyLimit() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = userPrincipal.getUserUuid();

        // Continue with your logic using userUuid
        return configService.getConfigsByUserUuid(userUuid).stream().filter(config -> config.getName().equals(UserConfig.DAILY_LIMIT.getValue())).map(Config::getValue).map(Integer::parseInt).findFirst().orElse(0);

    }

}
