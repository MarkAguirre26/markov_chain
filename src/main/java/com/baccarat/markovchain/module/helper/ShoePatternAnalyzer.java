package com.baccarat.markovchain.module.helper;

import java.util.ArrayList;
import java.util.List;

public class ShoePatternAnalyzer {

    public static Boolean analyzeShoe(String sequence) {
        List<Integer> groupLengths = new ArrayList<>();
        int count = 1;

        // Step 1: RLE - Group consecutive identical outcomes
        for (int i = 1; i < sequence.length(); i++) {
            if (sequence.charAt(i) == sequence.charAt(i - 1)) {
                count++;
            } else {
                groupLengths.add(count);
                count = 1;
            }
        }
        // Add the last group
        groupLengths.add(count);

        // Step 2: Analyze group lengths
        int shortStreaks = 0;  // For choppy (length 1 or 2)
        int longStreaks = 0;   // For volatile (length 3 or more)

        for (int length : groupLengths) {
            if (length <= 3) {
                shortStreaks++;
            } else {
                longStreaks++;
            }
        }

        // Step 3: Classification based on streak analysis
        double totalGroups = groupLengths.size();
        double shortStreaksPercentage = (shortStreaks / totalGroups) * 100;
        double longStreaksPercentage = (longStreaks / totalGroups) * 100;

        // Classify based on calculated percentages
        if (shortStreaksPercentage >= 70) {
            return true;  // Choppy
        } else if (longStreaksPercentage >= 30) {
            return false; // Volatile
        } else {
            return null;  // Mixed/Streaks or handle as needed
        }
    }

//    public static void main(String[] args) {
//
//        String shoeSequence = "";
//
//        // Analyze the shoe pattern
//        Boolean isSequenceChoppy = analyzeShoe(shoeSequence);
//
//        //if true then betSize will be 1u
////        but is tru use the Probability mm
//
//
//        // Display the result after each valid input
//        System.out.println("Current sequence: " + shoeSequence);
//        if (isSequenceChoppy != null) {
//            System.out.println("The shoe is classified as: " + (isSequenceChoppy ? "Choppy" : "Volatile"));
//        } else {
//            System.out.println("The shoe is classified as: Mixed/Streaks");
//        }
//
//
//    }

}
