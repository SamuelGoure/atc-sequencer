package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    @Id
    private String flightId;

    private String callSign;
    private String origin;
    private String destination;
    private LocalDateTime eta;

    @Enumerated(EnumType.STRING)
    private FlightStatus status = FlightStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    private FlightCategory category;

    private Integer sequenceNumber;

    private Integer delayMinutes = 0;

    private String runway;
}
