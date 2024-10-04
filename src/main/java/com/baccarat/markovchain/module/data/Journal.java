package com.baccarat.markovchain.module.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "journal")
public class Journal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journal_id")
    private Integer journalId;

    @Column(name = "user_uuid")
    private String userUuid;

    @Column(name = "shoe")
    private Integer shoe;

    @Column(name = "hand")
    private Integer hand;

    @Column(name = "profit")
    private Integer profit;

    @Column(name = "date_last_modified")
    private LocalDateTime dateLastModified;

    @Column(name = "date_created", updatable = false)
    private LocalDate dateCreated;
}
