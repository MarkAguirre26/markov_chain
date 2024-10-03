package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.model.Journal;
import com.baccarat.markovchain.module.model.Session;
import com.baccarat.markovchain.module.response.JournalResponse;
import com.baccarat.markovchain.module.services.JournalService;
import com.baccarat.markovchain.module.services.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/journals")
public class JournalController {

    private static final Logger logger = LoggerFactory.getLogger(JournalController.class);
    private final JournalService journalService;
    private final SessionService sessionService;

    @Autowired
    public JournalController(JournalService journalService, SessionService sessionService) {
        this.journalService = journalService;
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<Journal> createJournal(@RequestBody Journal journal) {
        logger.info("Creating journal: {}", journal);
        Journal savedJournal = journalService.saveJournal(journal);
        logger.info("Journal created: {}", savedJournal);
        return ResponseEntity.ok(savedJournal);
    }



    @GetMapping("/dateCreated")
    public ResponseEntity<List<JournalResponse>> getJournalsByDateCreated(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("dateCreated") LocalDate dateCreated) {

        logger.info("Authorization header: {}", authorizationHeader);
        logger.info("Date created: {}", dateCreated);


        String id  = "957baf71-80c4-11ef-a303-f02f748a05bf";
        Optional<Session> session = getSession(id);

        List<Journal> journals;
        if (session.isPresent()) {
            journals = journalService.getJournalsByUserUuidAndDateCreated(session.get().getUserUuid(), dateCreated);
        } else {
            // Handle the case where the session is not present
            logger.warn("Session not found for value: {}", authorizationHeader);
            return ResponseEntity.notFound().build();
        }

        int totalResponses = 10;
        // Create journal responses with a sequential shoe number
        List<JournalResponse> journalResponses = IntStream.range(0, journals.size())
                .mapToObj(i -> new JournalResponse(i + 1, journals.get(i).getHand(), journals.get(i).getProfit(), journals.get(i).getDateCreated()))
                .collect(Collectors.toList());

        // Fill in remaining responses if fewer than totalResponses
        int remainingShoe = journals.size() + 1;
        while (journalResponses.size() < totalResponses) {
            journalResponses.add(new JournalResponse(remainingShoe++, 0, 0.0, LocalDate.now()));
        }

        logger.info("Journals fetched: {}", journals.size());
        return ResponseEntity.ok(journalResponses);
    }

    private Optional<Session> getSession(String authorizationHeader) {
        String sessionValue = (authorizationHeader != null) ? authorizationHeader : ""; // Fallback to default session
        int isExpired = 0; // 0 -> not expired
        return sessionService.getSessionByValueAndExpired(sessionValue, isExpired);
    }

}
