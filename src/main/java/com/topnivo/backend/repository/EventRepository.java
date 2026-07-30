package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByAcknowledgedFalseOrderByEventDateDesc();

}
