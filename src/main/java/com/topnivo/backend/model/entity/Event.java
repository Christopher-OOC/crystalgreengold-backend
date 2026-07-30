package com.topnivo.backend.model.entity;

import com.topnivo.backend.model.constant.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String message;
    @Enumerated(EnumType.STRING)
    private EventType type;
    private boolean acknowledged;
    private LocalDateTime eventDate;

}
