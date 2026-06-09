package com.topnivo.backend.mapper;

import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.response.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreMapper {

    public StoreResponse storeToResponse(Member member) {

        return StoreResponse
                .builder()
                .storeId(member.getMemberId())
                .image(member.getImage())
                .businessName(member.getBusinessName())
                .address(member.getAddress())
                .phoneNumber(member.getPhoneNumber())
                .build();
    }
}
