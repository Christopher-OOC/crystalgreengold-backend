package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.entity.Event;
import com.topnivo.backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

        public List<Event> getUnacknowledgedEvents() {
        return eventRepository.findByAcknowledgedFalseOrderByEventDateDesc();
    }

    @Transactional
    public void acknowledgeEvent(long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchResourceException("Event not found"));

        event.setAcknowledged(true);

        eventRepository.save(event);
    }
}