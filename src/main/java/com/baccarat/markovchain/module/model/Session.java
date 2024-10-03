package com.baccarat.markovchain.module.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "session", schema = "markov_chain")
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", nullable = false)
    private Integer sessionId;

    @Column(name = "value", length = 254)
    private String value;

    @Column(name = "user_uuid", length = 254)
    private String userUuid;

    @Column(name = "expired")
    private Integer expired;

    @Column(name = "date_created", columnDefinition = "DATETIME DEFAULT CURDATE()")
    private LocalDateTime dateCreated;
}
