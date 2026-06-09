package com.topnivo.backend.mapper;

import com.topnivo.backend.model.request.MemberCreateRequest;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.response.MemberLittleResponse;
import com.topnivo.backend.model.response.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberMapper {

    private final ModelMapper modelMapper;

    public Member requestToCustomer(MemberCreateRequest request) {
        return Member
                .builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .username(request.getUsername())
                .accountListUsernames(new ArrayList<>())
                .canReceivePayment(true)
                .phoneNumber(request.getPhoneNumber())
                .build();
    }

    public MemberResponse memberToResponse(Member member) {
        MemberResponse memberResponse = modelMapper.map(member, MemberResponse.class);
        if (!Objects.isNull(member.getSponsor())) {
            memberResponse.setSponsorId(member.getSponsor().getMemberId());
            memberResponse.setSponsorUsername(member.getSponsor().getUsername());
        }
        if (!Objects.isNull(member.getPlacer())) {
            memberResponse.setPlacerId(member.getPlacer().getMemberId());
            memberResponse.setPlacerUsername(member.getPlacer().getUsername());
        }
        if (!Objects.isNull(member.getLeftLeg())) {
            memberResponse.setLeftLegId(member.getLeftLeg().getMemberId());
        }
        if (!Objects.isNull(member.getRightLeg())) {
            memberResponse.setRightLegId(member.getRightLeg().getMemberId());
        }

        return memberResponse;
    }

    public MemberLittleResponse memberToLittleResponse(Member member) {
        if (Objects.isNull(member)) {
            return null;
        }

        return modelMapper.map(member, MemberLittleResponse.class);
    }

    public MemberResponse memberToResponse(Member member, boolean setImageToNull) {
        MemberResponse memberResponse = modelMapper.map(member, MemberResponse.class);
        if (!Objects.isNull(member.getSponsor())) {
            memberResponse.setSponsorId(member.getSponsor().getMemberId());
        }
        if (!Objects.isNull(member.getPlacer())) {
            memberResponse.setPlacerId(member.getPlacer().getMemberId());
        }
        if (!Objects.isNull(member.getLeftLeg())) {
            memberResponse.setLeftLegId(member.getLeftLeg().getMemberId());
        }
        if (!Objects.isNull(member.getRightLeg())) {
            memberResponse.setRightLegId(member.getRightLeg().getMemberId());
        }

        member.setImage(null);

        return memberResponse;
    }
}
