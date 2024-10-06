package com.baccarat.markovchain.module.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;


import java.time.LocalDateTime;

@Entity
@Table(name = "game_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_status_id")
    private Integer gameStatusId;

    @Column(name = "game_response_id", columnDefinition = "int default 0")
    private int gameResponseId;

    @Column(name = "hand_count", columnDefinition = "int default 0")
    private Integer handCount;

    @Column(name = "wins", columnDefinition = "int default 0")
    private Integer wins;

    @Column(name = "losses", columnDefinition = "int default 0")
    private Integer losses;

    @Column(name = "profit", columnDefinition = "int default 0")
    private Integer profit;

    @Column(name = "playing_units", columnDefinition = "int default 100")
    private Integer playingUnits;

    @Column(name = "date_last_updated")
    private LocalDateTime dateLastUpdated;
}
