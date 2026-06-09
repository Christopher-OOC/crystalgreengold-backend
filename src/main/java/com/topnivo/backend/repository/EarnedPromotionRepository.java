package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.EarnedPromotion;
import com.topnivo.backend.model.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarnedPromotionRepository extends JpaRepository<EarnedPromotion, Integer> {

    List<EarnedPromotion> findByMember(Member member);

    EarnedPromotion findByNameAndMember(String name, Member member);

    Page<EarnedPromotion> findByHasReceived(boolean hasReceived, Pageable pageable);
}
