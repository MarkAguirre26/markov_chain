package com.baccarat.markovchain.module.data;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "trailing_stop")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrailingStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trialing_stop_id", nullable = false)
    private Integer trailingStopId;

    @Column(name = "user_uuid", length = 254)
    private String userUuid;

    @Column(name = "initial")
    private Integer initial;

    @Column(name = "trailing_percent")
    private Double trailingPercent;

    @Column(name = "stop_profit", precision = 25, scale = 0)
    private Double stopProfit;

    @Column(name = "highest_profit")
    private Integer highestProfit;


    public TrailingStop(String userUuid, int initialPrice, double trailingPercent, Double stopProfit, Integer highestProfit) {
        this.userUuid = userUuid;
        this.initial = initialPrice;
        this.trailingPercent = trailingPercent;
        this.stopProfit = stopProfit;
        this.highestProfit = highestProfit;

    }
}
