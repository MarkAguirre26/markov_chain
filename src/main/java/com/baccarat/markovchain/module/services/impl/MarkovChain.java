package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.model.Pair;
import org.springframework.stereotype.Service;


import java.util.*;

@Service
public class MarkovChain {


    /**
     * Predicts the next character based on the current character.
     * @return an Optional containing a Pair of the predicted character and its probability, or empty if no prediction is possible
     */
    public Optional<Pair<Character, Double>> predictNext(String sequence) {

        Map<Character, Map<Character, Integer>> transitions = new HashMap<>();
        Map<Character, Integer> totalCounts = new HashMap<>();


        if (sequence == null) {
            throw new IllegalArgumentException("Sequence cannot be null for training.");
        }
        for (int i = 0; i < sequence.length() - 1; i++) {
            char current = sequence.charAt(i);
            char next = sequence.charAt(i + 1);
            transitions
                    .computeIfAbsent(current, k -> new HashMap<>())
                    .merge(next, 1, Integer::sum);
            totalCounts.merge(current, 1, Integer::sum);
        }

        char currentChar = sequence.charAt(sequence.length() - 1);

        Map<Character, Integer> nextCharCounts = transitions.getOrDefault(currentChar, Collections.emptyMap());

        return nextCharCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> {
                    double probability = (double) entry.getValue() / totalCounts.get(currentChar);
                    return new Pair<>(entry.getKey(), probability);
                });
    }
}
