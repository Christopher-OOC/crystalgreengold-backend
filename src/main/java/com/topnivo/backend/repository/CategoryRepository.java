package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Category findByName(String name);

    Category findById(int id);

}
