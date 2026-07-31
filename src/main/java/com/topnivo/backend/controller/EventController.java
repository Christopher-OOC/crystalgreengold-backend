package com.topnivo.backend.controller;

import com.topnivo.backend.model.entity.Event;
import com.topnivo.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/unacknowledged")
    public ResponseEntity<List<Event>> getUnacknowledgedEvents() {
        return ResponseEntity.ok(eventService.getUnacknowledgedEvents());
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeEvent(@PathVariable long id) {

        eventService.acknowledgeEvent(id);

        return ResponseEntity.ok().build();
    }
}