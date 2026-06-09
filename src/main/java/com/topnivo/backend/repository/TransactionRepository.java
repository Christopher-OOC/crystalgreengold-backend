package com.topnivo.backend.repository;

import com.topnivo.backend.model.constant.TransactionType;
import com.topnivo.backend.model.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer>, PagingAndSortingRepository<Transaction, Integer> {

    List<Transaction> findByOrderId(String orderId);

    List<Transaction> findByReferenceId(String referenceId);

    Page<Transaction> findByOrderByTransactionDateDesc(Pageable pageable);

    Page<Transaction> findByMemberIdOrderByTransactionDateDesc(String memberId, Pageable pageable);

    Page<Transaction> findByTypeOrderByTransactionDateDesc(TransactionType type, Pageable pageable);

    Page<Transaction> findByMemberIdAndTypeOrderByTransactionDateDesc(String memberId, TransactionType type, Pageable pageable);

}
