package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Role findByName(String roleName);

    Role findById(int id);
}
