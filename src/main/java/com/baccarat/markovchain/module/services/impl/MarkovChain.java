package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.model.Pair;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class MarkovChain {
    private final Map<Character, Map<Character, Integer>> transitions = new HashMap<>();
    private final Map<Character, Integer> totalCounts = new HashMap<>();

    /**
     * Trains the Markov Chain model on the provided sequence of characters.
     *
     * @param sequence the sequence of characters to train on
     */
    public void train(String sequence) {
        for (int i = 0; i < sequence.length() - 1; i++) {
            char currentChar = sequence.charAt(i);
            char nextChar = sequence.charAt(i + 1);
            transitions
                    .computeIfAbsent(currentChar, k -> new HashMap<>())
                    .merge(nextChar, 1, Integer::sum);
            totalCounts.merge(currentChar, 1, Integer::sum);
        }
    }

    /**
     * Predicts the next character based on the current character.
     *
     * @param currentChar the current character
     * @return an Optional containing a Pair of the predicted character and its probability, or empty if no prediction is possible
     */
    public Optional<Pair<Character, Double>> predictNext(char currentChar) {
        Map<Character, Integer> nextCharCounts = transitions.getOrDefault(currentChar, Collections.emptyMap());

        return nextCharCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> {
                    double probability = (double) entry.getValue() / totalCounts.get(currentChar);
                    return new Pair<>(entry.getKey(), probability);
                });
    }
}
