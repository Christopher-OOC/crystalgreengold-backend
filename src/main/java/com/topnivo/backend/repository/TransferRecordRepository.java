package com.topnivo.backend.repository;

import com.topnivo.backend.model.constant.TransferStatus;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, Integer> {

    TransferRecord findByMember(Member member);

    TransferRecord findByReference(String reference);

    List<TransferRecord> findByStatus(TransferStatus status);

    List<TransferRecord> findByStatusIn(List<TransferStatus> statuses);

    void deleteByStatus(TransferStatus status);

    void deleteByStatusIn(List<TransferStatus> statuses);

}
