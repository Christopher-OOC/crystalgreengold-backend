package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.AdminSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSettingRepository extends JpaRepository<AdminSetting, Integer> {

    AdminSetting findById(int id);

    AdminSetting findByName(String name);

}
