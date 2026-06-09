package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.exception.exception.ResourceAlreadyExistException;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Rank;
import com.topnivo.backend.model.request.RankCreateRequest;
import com.topnivo.backend.repository.RankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RankService {

    private final RankRepository rankRepository;
    private final MemberService memberService;

    public Rank createRank(RankCreateRequest request) {
        checkIfAdminIsValid();

        Rank rank = rankRepository.findByName(request.getName());
        List<Rank> ranks = rankRepository.findAll();

        if (rank != null) {
            throw new ResourceAlreadyExistException(ErrorMessages.RANK_ALREADY_EXIST);
        }

        for (Rank checkRank : ranks) {
            if (checkRank.getRankValue() == request.getRankValue()) {
                throw new ResourceAlreadyExistException(ErrorMessages.RANK_VALUE_ALREADY_EXIST);
            }
        }

        Rank newRank = new Rank();
        newRank.setName(request.getName());
        newRank.setPrize(request.getPrize());
        newRank.setQualifyingBv(request.getQualifyingBv());
        newRank.setRankValue(request.getRankValue());

        return rankRepository.save(newRank);
    }

    public Rank updateRank(int id, RankCreateRequest request) {
        checkIfAdminIsValid();

        List<Rank> ranks = rankRepository.findAll();
        Optional<Rank> optional = rankRepository.findById(id);
        Rank rankWithTheName = rankRepository.findByName(request.getName());
        if (optional.isEmpty()) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_RANK);
        }

        Rank rank = optional.get();
        if (rankWithTheName != null) {
            if (rank.getId() != rankWithTheName.getId()) {
                throw new ResourceAlreadyExistException(ErrorMessages.RANK_ALREADY_EXIST);
            }
        }
        for (Rank checkRank : ranks) {
            if (rank.getId() != checkRank.getId() && checkRank.getRankValue() == request.getRankValue()) {
                throw new ResourceAlreadyExistException(ErrorMessages.RANK_VALUE_ALREADY_EXIST);
            }
        }

        rank.setName(request.getName());
        rank.setPrize(request.getPrize());
        rank.setRankValue(request.getRankValue());
        rank.setQualifyingBv(request.getQualifyingBv());

        return rankRepository.save(rank);
    }

    public List<Rank> findAllRanks() {

        return rankRepository.findAll();
    }

    public Rank findById(int id) {
        Optional<Rank> optional = rankRepository.findById(id);
        if (optional.isEmpty()) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_RANK);
        }

        return optional.get();
    }

    private void checkIfAdminIsValid() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdminSuperAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String adminUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        Member admin = memberService.findMemberByUsername(adminUserName);

        if (!isAdminSuperAdmin && admin.getSponsor() == null && admin.getPlacer() == null) {
            throw new BadRequestException(ErrorMessages.NO_SPONSOR_AND_PLACER);
        }
    }
}
