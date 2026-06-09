package com.topnivo.backend.repository;

import com.topnivo.backend.model.constant.CommissionType;
import com.topnivo.backend.model.entity.EarningCommission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EarningCommissionRepository extends JpaRepository<EarningCommission, Integer> {

    Page<EarningCommission> findByMemberIdOrderByEarnedDateDesc(String memberId, Pageable pageable);

    Page<EarningCommission> findByMemberIdAndCommissionTypeOrderByEarnedDateDesc(String memberId,
                                                                                 CommissionType commissionType,
                                                                                 Pageable pageable);
}
