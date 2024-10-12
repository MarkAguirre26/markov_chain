package com.baccarat.markovchain.module.helper;

import com.baccarat.markovchain.module.data.TrailingStop;
import com.baccarat.markovchain.module.services.TrailingStopService;
import org.springframework.beans.factory.annotation.Autowired;

public class TrailingStopHelper {

    @Autowired
    private TrailingStopService trailingStopService;

    public TrailingStopHelper(TrailingStopService trailingStopService) {
        this.trailingStopService = trailingStopService;
    }

    // Constructor
    public void initialize(String userUuid, int initialPrice, double trailingPercent) {

        double stopProfit = initialPrice * (1 - trailingPercent);
        trailingStopService.saveOrUpdate(new com.baccarat.markovchain.module.data.TrailingStop(userUuid, initialPrice, trailingPercent, stopProfit, initialPrice));

    }


    // Method to update the trailing stop based on the current price
    public String evaluateTrailingStop(String userUuid, int currentProfit) {


        TrailingStop trailingStop = trailingStopService.getTrailingStopByUserUuid(userUuid);

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
            return "StopProfitTriggered";
        } else {
            System.out.println("highestProfit: " + highestProfit);
            System.out.println("Current Profit: " + currentProfit + ", Stop Profit: " + stopProfit);
            return "Current Profit: " + currentProfit + ", Stop Profit: " + stopProfit;
        }
    }

}
