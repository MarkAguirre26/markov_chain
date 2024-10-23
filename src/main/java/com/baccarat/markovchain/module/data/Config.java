package com.baccarat.markovchain.module.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;


@Entity
@Table(name = "config")
@Data
@ToString
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private int configId;

    @Column(name = "user_uuid", length = 100)
    private String userUuid;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "value", length = 100)
    private String value;

    public Config(String userUuid, String name, String value) {
        this.userUuid = userUuid;
        this.name = name;
        this.value = value;
    }

    public Config() {

    }
}
