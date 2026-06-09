package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageRepository extends JpaRepository<Package, Integer> {

    Package findById(int id);

    Package findByName(String name);

}
