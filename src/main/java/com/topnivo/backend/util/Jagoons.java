package com.topnivo.backend.util;

import com.topnivo.backend.model.constant.CommissionType;
import com.topnivo.backend.model.entity.Commission;
import com.topnivo.backend.model.entity.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Jagoons {


    /*
        A Recursive method for finding all Sponsor down-lines
     */
    private void allAllDownlines(Member sponsor, List<Member> downlines) {
        if (Objects.isNull(sponsor.getLeftLeg()) && Objects.isNull(sponsor.getRightLeg())) {
            return;
        }
        else if (!Objects.isNull(sponsor.getLeftLeg()) && !Objects.isNull(sponsor.getRightLeg())) {
            downlines.add(sponsor.getLeftLeg());
            downlines.add(sponsor.getRightLeg());
            allAllDownlines(sponsor.getLeftLeg(), downlines);
            allAllDownlines(sponsor.getRightLeg(), downlines);
        }
        else if (Objects.isNull(sponsor.getLeftLeg())) {
            downlines.add(sponsor.getRightLeg());
            allAllDownlines(sponsor.getRightLeg(), downlines);
        }
        else {
            downlines.add(sponsor.getLeftLeg());
            allAllDownlines(sponsor.getLeftLeg(), downlines);
        }
    }

    public void oldMatching() {
//        // Get 1ST LEVEL Matching Commission
//        List<Member> members = findAllMembersByTheirSponsor(member);
//
//        double firstDirectMatching = members
//                .stream()
//                .mapToDouble(Member::getTotalDailyBv)
//                .map(v -> (commission1.getValue() / 100.0) * v)
//                .sum();
//
//        // Get 2ND LEVEL Matching Commission
//        List<Member> secondLevelMembers = new ArrayList<>();
//        members.forEach(m -> {
//            secondLevelMembers.addAll(findAllMembersByTheirSponsor(m));
//        });
//        Commission commission2 = commissionRepository.findByCommissionType(CommissionType.MATCHING_COMMISSION_LEVEL_2);
//        double secondDirectMatching = secondLevelMembers
//                .stream()
//                .mapToDouble(Member::getTotalDailyBv)
//                .map(v -> (commission2.getValue() / 100.0) * v)
//                .sum();
//
//        // Get 3RD LEVEL Matching Commission
//        List<Member> thirdLevelMembers = new ArrayList<>();
//        secondLevelMembers.forEach(m -> {
//            thirdLevelMembers.addAll(findAllMembersByTheirSponsor(m));
//        });
//        Commission commission3 = commissionRepository.findByCommissionType(CommissionType.MATCHING_COMMISSION_LEVEL_3);
//        double thirdDirectMatching = thirdLevelMembers
//                .stream()
//                .mapToDouble(Member::getTotalDailyBv)
//                .map(v -> (commission3.getValue() / 100.0) * v)
//                .sum();
//
//        double total = firstDirectMatching + secondDirectMatching + thirdDirectMatching;
//        member.setTotalBv(member.getTotalBv() + total);
//        memberRepository.save(member);
//
//        createMatchingCommission(member, total);
    }

    //    private void chooseLegForBinaryCommission(Member member, double leftPv, double rightPv) {
//        AdminSetting pvFactorSetting = adminSettingRepository.findByName(AdminSettings.PV_COMMISSION_FACTOR.name());
//        if (Objects.isNull(pvFactorSetting)) {
//            throw new NoSuchResourceException(ErrorMessages.ADMIN_SETTINGS_NOT_CREATED);
//        }
//
//        if (!Objects.isNull(member.getCurrentPackage())) {
//            double binaryCommissionRate = member.getCurrentPackage().getBinaryCommissionRate();
//            double weakLeg = Math.min(leftPv, rightPv);
//            log.info("WEAK LEG: {}", weakLeg);
//            double binaryPv = 0;
//            if (!hasMemberExceededDailyCapping(member)) {
//                if (weakLeg >= pvFactorSetting.getValue()) {
//                    if (leftPv > rightPv) {
//                        double weakLegFactor = ((int) (rightPv / pvFactorSetting.getValue())) * pvFactorSetting.getValue();
//                        binaryPv = (binaryCommissionRate / 100.0) * weakLegFactor;
//                        double remainingWeakLeg = rightPv - weakLegFactor;
//                        member.setTotalRightPv(remainingWeakLeg);
//                        member.setTotalLeftPv(leftPv - rightPv);
//                        log.info("L LEG > R LEG:");
//                        log.info("WeakLegFactor: {}", weakLegFactor);
//                    } else {
//                        double weakLegFactor = ((int) (leftPv / pvFactorSetting.getValue())) * pvFactorSetting.getValue();
//                        binaryPv = (binaryCommissionRate / 100.0) * weakLegFactor;
//                        double remainingWeakLeg = leftPv - weakLegFactor;
//                        member.setTotalRightPv(rightPv - leftPv);
//                        member.setTotalLeftPv(remainingWeakLeg);
//                        log.info("R LEG > L LEG:");
//                        log.info("WeakLegFactor: {}", weakLegFactor);
//                    }
//
//                    double value = addToAvailableBalance(member, binaryPv);
//                    memberRepository.save(member);
//                    createBinaryCommission(member, value);
//                    sendMatchingCommission(member, value);
//                }
//            }
//            else {
//                if (leftPv > rightPv) {
//                    member.setTotalRightPv(0);
//                } else {
//                    member.setTotalLeftPv(0);
//                }
//                memberRepository.save(member);
//            }
//        }
//    }

    //@Async
//    public void sendBinaryCommission(Member member) {
//        if (Objects.isNull(member)) {
//            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
//        }
//
//        Member leftLeg = member.getLeftLeg();
//        Member rightLeg = member.getRightLeg();
//        List<Member> leftLegDownLines = findAllMemberDownLines(leftLeg);
//        List<Member> rightLegDownLines = findAllMemberDownLines(rightLeg);
//
//        double totalLeftLegDownlinesPv = leftLegDownLines
//                .stream()
//                .mapToDouble(m -> (m.getTotalLeftPv() + m.getTotalRightPv()))
//                .sum();
//        double totalRightLegDownlinesPv = rightLegDownLines
//                .stream()
//                .mapToDouble(m -> (m.getTotalLeftPv() + m.getTotalRightPv()))
//                .sum();
//
//        if (!Objects.isNull(leftLeg) && !Objects.isNull(rightLeg)) {
//            double leftPv = (leftLeg.getTotalLeftPv() + leftLeg.getTotalRightPv()) + totalLeftLegDownlinesPv;
//            double rightBv = (rightLeg.getTotalLeftPv() + rightLeg.getTotalRightPv()) + totalRightLegDownlinesPv;
//            chooseLegForBinaryCommission(member, leftPv, rightBv);
//        }
//        else if (!Objects.isNull(leftLeg)) {
//            double leftPv = (leftLeg.getTotalLeftPv() + leftLeg.getTotalRightPv()) + totalLeftLegDownlinesPv;
//            chooseLegForBinaryCommission(member, leftPv, 0);
//        }
//        else if (!Objects.isNull(rightLeg)) {
//            double rightBv = (rightLeg.getTotalLeftPv() + rightLeg.getTotalRightPv()) + totalRightLegDownlinesPv;
//            chooseLegForBinaryCommission(member, 0, rightBv);
//        }
//    }
}
