package com.baccarat.markovchain.module.services.impl;

import org.springframework.stereotype.Service;

@Service
public class PatternRecognizer {
    public String findPattern(String sequence) {
        int len = sequence.length();
        for (int patternLength = 1; patternLength <= len / 2; patternLength++) {
            String pattern = sequence.substring(len - patternLength);
            if (sequence.endsWith(pattern.repeat(2))) return pattern;
        }
        return null;
    }
}