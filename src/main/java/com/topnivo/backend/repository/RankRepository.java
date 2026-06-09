package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankRepository extends JpaRepository<Rank, Integer> {

    Rank findByName(String name);

}
