package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.model.Pair;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class MarkovChain {
    private final Map<Character, Map<Character, Integer>> transitions = new HashMap<>();
    private final Map<Character, Integer> totalCounts = new HashMap<>();

    public void train(String sequence) {
        for (int i = 0; i < sequence.length() - 1; i++) {
            char currentChar = sequence.charAt(i), nextChar = sequence.charAt(i + 1);
            transitions.computeIfAbsent(currentChar, k -> new HashMap<>()).merge(nextChar, 1, Integer::sum);
            totalCounts.merge(currentChar, 1, Integer::sum);
        }
    }

    public Pair<Character, Double> predictNext(char currentChar) {
        return transitions.getOrDefault(currentChar, Collections.emptyMap()).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new Pair<>(entry.getKey(), (double) entry.getValue() / totalCounts.get(currentChar)))
                .orElse(null);
    }
}