package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.constant.TransactionType;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Transaction;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;

    public Page<Transaction> getTransactionByPage(String memberId, int page, int size, String type) {
        Member member = memberRepository.findByMemberId(memberId);
        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        String role = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> !r.equals("ROLE_REGULAR_MEMBER"))
                .findFirst()
                .orElse("ROLE_REGULAR_MEMBER");

        boolean canViewAllTransactions = role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN");

        Pageable pageable = PageRequest.of(page -1, size);

        if (type.equals("ALL")) {
            if (canViewAllTransactions) {
                return transactionRepository.findByOrderByTransactionDateDesc(pageable);
            }
            return transactionRepository.findByMemberIdOrderByTransactionDateDesc(memberId, pageable);
        }
        else {
            TransactionType transactionType = null;
            try {
                transactionType = TransactionType.valueOf(type);
            }
            catch (Exception ex) {
                throw new BadRequestException(ErrorMessages.NO_SUCH_TRANSACTION_TYPE);
            }

            if (canViewAllTransactions) {
                return transactionRepository.findByTypeOrderByTransactionDateDesc(transactionType, pageable);
            }
            return transactionRepository.findByMemberIdAndTypeOrderByTransactionDateDesc(memberId, transactionType, pageable);
        }
    }
}
