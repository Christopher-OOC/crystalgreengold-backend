package com.topnivo.backend.controller;

import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.service.CommissionService;
import com.topnivo.backend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/test")
@RequiredArgsConstructor
public class TestController {

    private final CommissionService commissionService;
    private final MemberService memberService;

    @GetMapping(value = "/binary/{memberId}")
    public String sendBinaryCommission(@PathVariable("memberId") String memberId) {
        Member member = memberService.findMemberByMemberId(memberId);
        commissionService.sendUpLineBinaryCommission(member);

        return "SUCCESS";

    }

}
