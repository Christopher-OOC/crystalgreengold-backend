package com.topnivo.backend.repository;

import com.topnivo.backend.model.constant.CommissionType;
import com.topnivo.backend.model.entity.Commission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRepository extends JpaRepository<Commission, Integer> {

    Commission findByCommissionType(CommissionType commissionType);

}
