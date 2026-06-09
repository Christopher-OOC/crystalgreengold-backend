package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.model.constant.CommissionType;
import com.topnivo.backend.model.entity.EarningCommission;
import com.topnivo.backend.repository.EarningCommissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarningCommissionService {

    private final EarningCommissionRepository earningCommissionRepository;

    public Page<EarningCommission> getBonusesByPage(String memberId, int page, int size, String type) {
        Pageable pageable = PageRequest.of(page -1, size);

        if (type.equals("ALL")) {
            return earningCommissionRepository.findByMemberIdOrderByEarnedDateDesc(memberId, pageable);
        }
        else {
            CommissionType commissionType = null;
            try {
                commissionType = CommissionType.valueOf(type);
            }
            catch (Exception ex) {
                throw new BadRequestException(ErrorMessages.NO_SUCH_COMMISSION_TYPE);
            }
            return earningCommissionRepository.findByMemberIdAndCommissionTypeOrderByEarnedDateDesc(memberId, commissionType, pageable);
        }
    }
}
