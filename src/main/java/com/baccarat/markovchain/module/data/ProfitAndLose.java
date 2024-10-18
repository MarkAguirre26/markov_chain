package com.baccarat.markovchain.module.data;

import lombok.Data;

import java.util.List;

@Data
public class ProfitAndLose {
    private List<String> labels;  // Array of labels
    private List<Integer> data;       // Array of data values

    public ProfitAndLose(List<String> labels, List<Integer> data) {
        this.labels = labels;
        this.data = data;
    }
}