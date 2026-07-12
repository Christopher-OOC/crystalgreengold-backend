package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.constant.*;
import com.topnivo.backend.model.entity.*;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CommissionService {

    private final MemberRepository memberRepository;
    private final EarningCommissionRepository earningCommissionRepository;
    private final AdminSettingRepository adminSettingRepository;
    private final TransactionRepository transactionRepository;
    private final PromotionRepository promotionRepository;
    private final EarnedPromotionRepository earnedPromotionRepository;
    private final RankRepository rankRepository;
    private final PackageRepository packageRepository;

    private static final String AFRICA_LAGOS_TIMEZONE = "Africa/Lagos";
    private static final String MONTHLY_RESET_CRON = "0 0 0 1 * ?";
    private static final String DAILY_AT_MIDNIGHT = "0 0 0 * * ?";

    @Async
    public void sendDirectAndIndirectReferralCommission(Member member, double pv) {
        for (int level = 1; level <= 6; level++) {
            sendReferralCommission(member, pv, level);
        }
    }

    @Async
    private void sendReferralCommission(Member member, double pv, int level) {
        Member sponsor = getSponsorForLevel(member, level);
        if (sponsor == null) return;

        double commissionRate = getCommissionRateForLevel(sponsor, level);
        if (commissionRate <= 0) return;

        double calculatedBonus = (commissionRate / 100.0) * pv;
        addBonusToBalanceAndCreateCommissionRecord(level, member, sponsor, calculatedBonus);
    }

    private void addBonusToBalanceAndCreateCommissionRecord(int level, Member member, Member sponsor, double calculatedBonus) {
        double value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(sponsor, calculatedBonus);
        createReferralCommission(member, sponsor, value, level);
    }

    @Async
    private void createReferralCommission(Member member, Member sponsor, double value, int level) {
        String referralType = switch (level) {
            case 1 -> "direct";
            case 2 -> "first level";
            case 3 -> "second level";
            case 4 -> "third level";
            case 5 -> "fourth level";
            case 6 -> "fifth level";
            default -> level + "th level";
        };

        EarningCommission earningCommission = new EarningCommission();
        earningCommission.setMemberId(sponsor.getMemberId());
        earningCommission.setAmount(value);
        earningCommission.setDescription(String.format("You earned %s commission from %s as a %s referral.", value, member.getUsername(), referralType));
        earningCommission.setEarnedDate(LocalDateTime.now());
        earningCommission.setCommissionType(
                level == 1 ? CommissionType.DIRECT_REFERRAL : CommissionType.INDIRECT_REFERRAL
        );

        earningCommissionRepository.save(earningCommission);
    }

    private double addPvMonetaryValueToAvailableBalance(Member member, double amountPv) {
        double pvEquivalence = getAdminSettingValue(AdminSettings.PV_EQUIVALENCE);
        double value = roundToTwoDecimalPlaces(pvEquivalence * amountPv);

        member.setAvailableBalance(Math.abs(member.getAvailableBalance() + value));
        member.setLastEarned(new Date());
        memberRepository.save(member);

        return value;
    }

    private Member getSponsorForLevel(Member member, int level) {
        Member sponsor = member;
        for (int i = 0; i < level && sponsor != null; i++) {
            sponsor = sponsor.getSponsor();
        }
        return sponsor;
    }

    private double getCommissionRateForLevel(Member sponsor, int level) {
        String packageName = sponsor.getCurrentPackage() == null
                ?
                PackageName.FREE.name()
                :
                sponsor.getCurrentPackage().getName();

        switch (packageName) {
            case "FREE", "STARTER", "BASIC", "BRONZE", "SILVER", "GOLD", "DIAMOND", "PLATINUM":
                return switch (level) {
                    case 1 -> 30;
                    case 2 -> 3;
                    case 3 -> 1;
                    case 4 -> 1;
                    case 5 -> 1;
                    case 6 -> 1;
                    default -> 0;
                };
            default:
               return 0;
        }
    }

    private double getAdminSettingValue(AdminSettings setting) {
        AdminSetting adminSetting = adminSettingRepository.findByName(setting.name());
        return adminSetting != null ? adminSetting.getValue() : 0;
    }

    public void sendUpLineBinaryCommission(Member member) {
        if (member == null) {
            return;
        }

        Member placer = member;
        while (placer != null) {
            processBinaryCommissionForMember(placer);
            placer = placer.getPlacer();
        }
    }

    public void processBinaryCommissionForMember(Member member) {
        if (member == null) return;

        double pvFactor = getAdminSettingValue(AdminSettings.PV_COMMISSION_FACTOR);
        if (pvFactor <= 0) {
            throw new IllegalStateException("PV commission factor must be > 0");
        }

        Package freePackage = packageRepository.findByName(PackageName.FREE.name());
        double binaryCommissionRate = Optional.ofNullable(member.getCurrentPackage())
                .map(Package::getBinaryCommissionRate)
                .orElseGet(freePackage::getBinaryCommissionRate);

        if (member.getLastEarned() == null) {
            member.setLastEarned(new Date());
            member.setDailyBinaryEarning(0.0);
            memberRepository.save(member);
        }

        boolean isSameDay = DateUtils.isSameDay(new Date(), member.getLastEarned());

        if (!isSameDay) {
            member.setDailyBinaryEarning(0.0);
            member.setLastEarned(new Date());
            memberRepository.save(member);
        }

        double dailyCapping = Optional.ofNullable(member.getCurrentPackage())
                .map(Package::getDailyCapping)
                .orElseGet(freePackage::getDailyCapping);

        if (member.getDailyBinaryEarning() >= dailyCapping) {
            resetMemberLegsAfterCapping(member);
            memberRepository.save(member);
            return;
        }

        double weakLeg = Math.min(member.getBinaryLeftPv(), member.getBinaryRightPv());
        if (weakLeg >= pvFactor) {
            calculateAndDistributeBinaryCommission(member, pvFactor, binaryCommissionRate, dailyCapping);
        }
    }

    private void resetMemberLegsAfterCapping(Member member) {
        if (member.getBinaryLeftPv() > member.getBinaryRightPv()) {
            member.setBinaryRightPv(0.0);
        }
        else {
            member.setBinaryLeftPv(0.0);
        }
    }

    @Transactional
    protected void calculateAndDistributeBinaryCommission(Member member,
                                                          double pvFactor,
                                                          double commissionRate,
                                                          double dailyCapping) {
        double leftPv = member.getBinaryLeftPv();
        double rightPv = member.getBinaryRightPv();

        if (leftPv <= 0 && rightPv <= 0) return;

        double weakerLeg = Math.min(leftPv, rightPv);
        long units = (long) (weakerLeg / pvFactor);
        if (units <= 0) return;

        double weakLegFactor = units * pvFactor;

        double binaryPvValue = (commissionRate / 100.0) * weakLegFactor;

        member.setBinaryLeftPv(Math.abs(leftPv - weakLegFactor));
        member.setBinaryRightPv(Math.abs(rightPv - weakLegFactor));

        double value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(member, binaryPvValue);

        member.setDailyBinaryEarning(Math.abs(member.getDailyBinaryEarning() + value));
        member.setLastEarned(new Date());

        if (member.getDailyBinaryEarning() >= dailyCapping) {
            resetMemberLegsAfterCapping(member);
        }

        memberRepository.save(member);

        createCommissionRecord(member, value, String.format("You earned %s as a binary commission!", value), CommissionType.BINARY_COMMISSION);

        sendMatchingCommission(member, binaryPvValue);
    }

    @Async
    private void sendMatchingCommission(Member member, double amountPv) {
        Member level1Sponsor = member.getSponsor();
        if (level1Sponsor != null) {
            sendMatchingCommissionForLevel(level1Sponsor, amountPv, 1, AdminSettings.MATCHING_COMMISSION_LEVEL_1);
            Member level2Sponsor = level1Sponsor.getSponsor();
            if (level2Sponsor != null) {
                sendMatchingCommissionForLevel(level2Sponsor, amountPv, 2, AdminSettings.MATCHING_COMMISSION_LEVEL_2);

                Member level3Sponsor = level2Sponsor.getSponsor();
                if (level3Sponsor != null) {
                    sendMatchingCommissionForLevel(level3Sponsor, amountPv, 3, AdminSettings.MATCHING_COMMISSION_LEVEL_3);
                }
            }
        }
    }

    @Async
    private void sendMatchingCommissionForLevel(Member sponsor, double amountPv, int level, AdminSettings setting) {
        if (sponsor == null) {
            return;
        }

        double commissionRate = getAdminSettingValue(setting);
        double commissionAmount = (commissionRate / 100.0) * amountPv;
        double value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(sponsor, commissionAmount);
        createCommissionRecord(
                sponsor,
                value,
                String.format("You earned %s matching commission.", value),
                CommissionType.MATCHING_COMMISSION
        );
    }

    @Async
    public void sendUniLevelCommission(Member member, double totalBoughtPv) {
        if (member == null) {
            return;
        }
        Package freePackage = packageRepository.findByName(PackageName.FREE.name());

        String name = member.getCurrentPackage() != null ? member.getCurrentPackage().getName() : freePackage.getName();
        PackageName packageName = PackageName.valueOf(name);

        switch (packageName) {
            case FREE, STARTER -> processEntryPackageUnilevel(member, totalBoughtPv);
            case BASIC -> processBasicPackageUnilevel(member, totalBoughtPv);
            case BRONZE -> processBronzePackageUnilevel(member, totalBoughtPv);
            case SILVER -> processSilverPackageUnilevel(member, totalBoughtPv);
            case GOLD -> processGoldPackageUnilevel(member, totalBoughtPv);
            case PLATINUM -> processPlatinumPackageUnilevel(member, totalBoughtPv);
            case DIAMOND -> processDiamondPackageUnilevel(member, totalBoughtPv);
        }
    }

    @Async
    private void processEntryPackageUnilevel(Member member, double totalBoughtPv) {
        // Direct repurchase bonus (30%)
        double prb = (30.0 / 100) * totalBoughtPv;
        sendDirectRepurchaseBonus(member, prb);

        // Level bonuses (Gen1-Gen3)
        Map<String, Integer> levelBonuses = Map.of(
                "Gen1", 1,
                "Gen2", 1,
                "Gen3", 1
        );

        distributeUnilevelCommissions(member, totalBoughtPv, levelBonuses, 3);
    }

    @Async
    private void processBasicPackageUnilevel(Member member, double totalBoughtPv) {
        // Direct repurchase bonus (30%)
        double prb = (30.0 / 100) * totalBoughtPv;
        sendDirectRepurchaseBonus(member, prb);

        // Level bonuses (Gen1-Gen5)
        Map<String, Integer> levelBonuses = Map.of(
                "Gen1", 1,
                "Gen2", 1,
                "Gen3", 1,
                "Gen4", 1,
                "Gen5", 1
        );

        distributeUnilevelCommissions(member, totalBoughtPv, levelBonuses, 5);
    }

    @Async
    private void processBronzePackageUnilevel(Member member, double totalBoughtPv) {
        // Direct repurchase bonus (30%)
        double prb = (30.0 / 100) * totalBoughtPv;
        sendDirectRepurchaseBonus(member, prb);

        // Level bonuses (Gen1-Gen6)
        Map<String, Integer> levelBonuses = Map.of(
                "Gen1", 1,
                "Gen2", 1,
                "Gen3", 1,
                "Gen4", 1,
                "Gen5", 1,
                "Gen6", 1
        );

        distributeUnilevelCommissions(member, totalBoughtPv, levelBonuses, 6);
    }


    @Async
    private void processSilverPackageUnilevel(Member member, double totalBoughtPv) {
        // Direct repurchase bonus (30%)
        double prb = (30.0 / 100) * totalBoughtPv;
        sendDirectRepurchaseBonus(member, prb);

        // Level bonuses (Gen1-Gen7)
        Map<String, Integer> levelBonuses = Map.of(
                "Gen1", 1,
                "Gen2", 1,
                "Gen3", 1,
                "Gen4", 1,
                "Gen5", 1,
                "Gen6", 1,
                "Gen7", 1
        );

        distributeUnilevelCommissions(member, totalBoughtPv, levelBonuses, 7);
    }

    @Async
    private void processGoldPackageUnilevel(Member member, double totalBoughtPv) {
        // Direct repurchase bonus (30%)
        double prb = (30.0 / 100) * totalBoughtPv;
        sendDirectRepurchaseBonus(member, prb);

        // Level bonuses (Gen1-Gen9)
        Map<String, Integer> levelBonuses = Map.of(
                "Gen1", 1,
                "Gen2", 1,
                "Gen3", 1,
                "Gen4", 1,
                "Gen5", 1,
                "Gen6", 1,
                "Gen7", 1,
                "Gen8", 1,
                "Gen9", 1
        );

        distributeUnilevelCommissions(member, totalBoughtPv, levelBonuses, 9);
    }

    @Async
    private void processPlatinumPackageUnilevel(Member member, double totalBoughtPv) {
        // Direct repurchase bonus (30%)
        double prb = (30.0 / 100) * totalBoughtPv;
        sendDirectRepurchaseBonus(member, prb);

        // Level bonuses (Gen1-Gen10)
        Map<String, Integer> levelBonuses = Map.of(
                "Gen1", 1,
                "Gen2", 1,
                "Gen3", 1,
                "Gen4", 1,
                "Gen5", 1,
                "Gen6", 1,
                "Gen7", 1,
                "Gen8", 1,
                "Gen9", 1,
                "Gen10", 1
        );

        distributeUnilevelCommissions(member, totalBoughtPv, levelBonuses, 10);
    }

    @Async
    private void processDiamondPackageUnilevel(Member member, double totalBoughtPv) {
        // Direct repurchase bonus (30%)
        double prb = (30.0 / 100) * totalBoughtPv;
        sendDirectRepurchaseBonus(member, prb);

        // Level bonuses (Gen1-Gen12)
        Map<String, Integer> levelBonuses = new LinkedHashMap<>();
        levelBonuses.put("Gen1", 1);
        levelBonuses.put("Gen2", 1);
        levelBonuses.put("Gen3", 1);
        levelBonuses.put("Gen4", 1);
        levelBonuses.put("Gen5", 1);
        levelBonuses.put("Gen6", 1);
        levelBonuses.put("Gen7", 1);
        levelBonuses.put("Gen8", 1);
        levelBonuses.put("Gen9", 1);
        levelBonuses.put("Gen10", 1);
        levelBonuses.put("Gen11", 1);
        levelBonuses.put("Gen12", 1);

        distributeUnilevelCommissions(member, totalBoughtPv, levelBonuses, 12);
    }

    @Async
    private void distributeUnilevelCommissions(Member member, double totalBoughtPv,
                                               Map<String, Integer> levelBonuses, int maxLevel) {
        Member sponsor = member.getSponsor();
        for (int i = 1; i <= maxLevel; i++) {
            if (sponsor == null) {
                break;
            }

            double prbValue = (levelBonuses.get("Gen" + i) / 100.0) * totalBoughtPv;
            sendIndirectRepurchaseBonus(sponsor, prbValue);
            sponsor = sponsor.getSponsor();
        }
    }

    @Async
    private void sendDirectRepurchaseBonus(Member member, double prb) {
        if (member == null) {
            return;
        }

        double value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(member, prb);
        memberRepository.save(member);
        createRepurchaseCommission(member, value);
    }

    @Async
    private void sendIndirectRepurchaseBonus(Member member, double prb) {
        if (member == null) {
            return;
        }

        double value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(member, prb);
        memberRepository.save(member);
        createUniLevelCommission(member, value);
    }

    @Async
    private void createRepurchaseCommission(Member member, double amount) {
        createCommissionRecord(
                member,
                amount,
                String.format("You earned %s repurchase commission.", amount),
                CommissionType.REPUCHASE_COMMISSION
        );
    }

    @Async
    private void createUniLevelCommission(Member member, double amount) {
        createCommissionRecord(
                member,
                amount,
                String.format("You earned %s uni-level commission.", amount),
                CommissionType.UNILEVEL_COMMISSION
        );
    }

    private double addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(Member member, double amountPv) {
        tryGivingRankToMember(member);

        double pvEquivalence = getAdminSettingValue(AdminSettings.PV_EQUIVALENCE);
        double value = roundToTwoDecimalPlaces(pvEquivalence * amountPv);
        double toAvailableBalance = (90.01 / 100) * value;
//        double toAvailableBalance = value;
        double toAwaitingWallet = Math.abs(value - toAvailableBalance);

        member.setAvailableBalance(Math.abs(member.getAvailableBalance() + toAvailableBalance));
        member.setAwaitingWallet(Math.abs(member.getAwaitingWallet() + toAwaitingWallet));
        member.setLastEarned(new Date());
        memberRepository.save(member);

        return value;
    }

    private double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Transactional
    public void tryGivingRankToMember(Member member) {
        if (member == null) {
            return;
        }

        List<Rank> ranks = rankRepository.findAllByOrderByRankValueAsc();

        double weakLegBv = Math.min(member.getTotalLeftBv(), member.getTotalRightBv());

        Rank qualifiedRank = member.getRank();

        for (Rank rank : ranks) {
            if (weakLegBv >= rank.getQualifyingBv()) {
                qualifiedRank = rank;
            } else {
                break;
            }
        }

        if (qualifiedRank == null) {
            return;
        }

        if (member.getRank() != null &&
                Objects.equals(member.getRank().getId(), qualifiedRank.getId())) {
            return;
        }

        if (member.getRank() != null &&
                member.getRank().getRankValue() >= qualifiedRank.getRankValue()) {
            return;
        }

        member.setRank(qualifiedRank);

        double value;

        if (member.getRank() == null) {
            value = addMonetaryValueToAvailableBalance(member, qualifiedRank.getPrize());
        } else {
            value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(member, qualifiedRank.getPrize());
        }

        memberRepository.save(member);

        createCommissionRecord(
                member,
                value,
                "You have earned " + value + " by getting Rank (" + qualifiedRank.getName() + ")!",
                CommissionType.RANK_COMMISSION
        );

        sendLeadershipCelebrationBonus(member, qualifiedRank.getPrize());
    }

    public void sendLeadershipCelebrationBonus(Member member, double amount) {
        if (member == null) {
            return;
        }
        Member directSponsor = member.getSponsor();
        if (directSponsor != null) {
            sendLeadershipCelebrationBonusToSponsor(directSponsor, amount, 10);
            Member secondLevelSponsor = directSponsor.getSponsor();
            sendLeadershipCelebrationBonusToSponsor(secondLevelSponsor, amount, 5);
        }
    }

    public void sendLeadershipCelebrationBonusToSponsor(Member sponsor, double amount, double percentage) {
        if (sponsor == null) {
            return;
        } else {
            double value = (percentage / 100.0) * amount;
            double added = addMonetaryValueToAvailableBalance(sponsor, value);
            memberRepository.save(sponsor);

            createCommissionRecord(sponsor,
                    added,
                    "You have earned " + value + " leadership celebration bonus!",
                    CommissionType.LEADERSHIP_CELEBRATION_COMMISSION);
        }
    }

    @Async
    public void tryGiveEligibleMemberPromo() {
        List<Member> members = memberRepository.findAll();
        List<Promotion> promotions = promotionRepository.findAll()
                .stream()
                .sorted(Comparator.comparingDouble(Promotion::getTargetPv))
                .toList();

        for (Member member : members) {
            for (Promotion promotion : promotions) {
                if (promotion.getEndDate().isAfter(LocalDateTime.now())) {
                    promotionRepository.deleteById(promotion.getId());
                } else {
                    EarnedPromotion alreadyEarnedPromo = earnedPromotionRepository.findByNameAndMember(promotion.getName(), member);
                    if (promotion.isEnabled() && alreadyEarnedPromo == null) {
                        checkAndCreatePromotion(member, promotion);
                    }
                }
            }
        }
    }

    private void checkAndCreatePromotion(Member member, Promotion promotion) {
        if (member.getMonthlyLeftPv() > member.getMonthlyRightPv()) {
            if (member.getMonthlyRightPv() >= promotion.getTargetPv()) {
                createEarnedPromotion(member, promotion);
            }
        } else {
            if (member.getMonthlyLeftPv() >= promotion.getTargetPv()) {
                createEarnedPromotion(member, promotion);
            }
        }
    }

    private void createEarnedPromotion(Member member, Promotion promotion) {

        EarnedPromotion earnedPromotion = new EarnedPromotion();
        earnedPromotion.setName(promotion.getName());
        earnedPromotion.setDescription(promotion.getDescription());
        earnedPromotion.setImage(promotion.getImage());
        earnedPromotion.setDateEarned(LocalDateTime.now());
        earnedPromotion.setHasReceived(false);
        earnedPromotion.setTargetPv(promotion.getTargetPv());
        earnedPromotion.setPrize(promotion.getPrize());
        earnedPromotion.setMember(member);

        earnedPromotionRepository.save(earnedPromotion);
    }

    @Async
    @Scheduled(cron = MONTHLY_RESET_CRON, zone = AFRICA_LAGOS_TIMEZONE)
    public void sendNewlyRegisteredMemberCounterCommission() {
        AdminSetting countSetting = adminSettingRepository.findByName(AdminSettings.MONTHLY_NEWLY_REGISTERED_ON_WEAK_LEG_NUMBER.name());
        AdminSetting priceSetting = adminSettingRepository.findByName(AdminSettings.MONTHLY_NEWLY_REGISTERED_ON_WEAK_LEG_PRICE.name());
        if (countSetting == null || priceSetting == null) {
            throw new NoSuchResourceException(ErrorMessages.ADMIN_SETTINGS_NOT_CREATED);
        }

        List<Member> members = memberRepository.findAll();
        for (Member member : members) {
            if (member.getCountNewlyRegisteredOnMonthlyWeakerLeg() >= countSetting.getValue()) {
                addMonetaryValueToAvailableBalance(member, priceSetting.getValue());

                createNewlyRegisteredMemberCounterCommission(member, priceSetting.getValue());
            }
        }
    }

    @Async
    private void createNewlyRegisteredMemberCounterCommission(Member member, double amount) {
        createCommissionRecord(member, amount,
                String.format("You earned %s newly registered counter commission.", amount),
                CommissionType.NEWLY_REGISTERED_COUNTER_COMMISSION);
    }

    public void resetMemberAtTheEndOfTheMonth() {
        List<Member> members = memberRepository.findAll();
        for (Member member : members) {
            if (member.getAwaitingWallet() > 0) {
                addAwaitingBalanceToAvailableBalance(member);
            }
            member.setMonthlyRightPv(0.0);
            member.setMonthlyLeftPv(0.0);
            member.setCountNewlyRegisteredOnMonthlyWeakerLeg(0);
            member.setAwaitingWallet(0.0);
        }

        memberRepository.saveAll(members);
    }

    public void addAwaitingBalanceToAvailableBalance(Member member) {
        ZonedDateTime lagosTimeNow = ZonedDateTime.now(ZoneId.of(AFRICA_LAGOS_TIMEZONE));
        if (member.getLastActive().getMonthValue() == lagosTimeNow.getMonthValue()) {
            addMonetaryValueToAvailableBalance(member, member.getAwaitingWallet());
            member.setAwaitingWallet(0.0);
            member.setLastActive(LocalDateTime.now());
            member.setLastEarned(new Date());
            memberRepository.save(member);
        }
    }

    public void updateSponsorNewRegistrationCount(Member member) {
        Member sponsor = member.getSponsor();

        if (sponsor == null) return;

        Member left = sponsor.getLeftLeg();
        Member right = sponsor.getRightLeg();

        if (left == null && right == null) return;

        double leftPv = sponsor.getMonthlyLeftPv();
        double rightPv = sponsor.getMonthlyRightPv();

        boolean isLeftWeaker = leftPv < rightPv;
        boolean isRightWeaker = rightPv < leftPv;

        if (isLeftWeaker) {
            sponsor.setCountNewlyRegisteredOnMonthlyWeakerLeg(
                    sponsor.getCountNewlyRegisteredOnMonthlyWeakerLeg() + 1
            );
            memberRepository.save(sponsor);
        }

        if (isRightWeaker) {
            sponsor.setCountNewlyRegisteredOnMonthlyWeakerLeg(
                    sponsor.getCountNewlyRegisteredOnMonthlyWeakerLeg() + 1
            );
            memberRepository.save(sponsor);
        }
    }

    public void addBinaryBvAndPvToAllUpLines(Member member, double bv, double pv) {
        if (member == null) return;

        List<Member> upLines = new ArrayList<>();
        Member currentMember = member;
        Member currentUpLine = member.getPlacer();

        while (currentUpLine != null) {
            updateBinaryUpLineLegValues(currentMember, currentUpLine, bv, pv);
            upLines.add(currentUpLine);
            currentMember = currentUpLine;
            currentUpLine = currentMember.getPlacer();
        }

        if (!upLines.isEmpty()) {
            memberRepository.saveAll(upLines);
        }

        sendUpLineBinaryCommission(member);
    }

    private void updateBinaryUpLineLegValues(Member downLine, Member upLine, double bv, double pv) {
        if (downLine == null || upLine == null) return;

        Member leftLeg = upLine.getLeftLeg();

        if (leftLeg != null && downLine.getId() == leftLeg.getId()) {
            upLine.setTotalLeftBv(Math.abs(upLine.getTotalLeftBv() + bv));
            upLine.setBinaryLeftPv(Math.abs(upLine.getBinaryLeftPv() + pv));
            upLine.setMonthlyLeftPv(Math.abs(upLine.getMonthlyLeftPv() + pv));
        } else {
            upLine.setTotalRightBv(Math.abs(upLine.getTotalRightBv() + bv));
            upLine.setBinaryRightPv(Math.abs(upLine.getBinaryRightPv() + pv));
            upLine.setMonthlyRightPv(Math.abs(upLine.getMonthlyRightPv() + pv));
        }

        upLine = memberRepository.save(upLine);
        tryGivingRankToMember(upLine);
    }

    private double addMonetaryValueToAvailableBalance(Member member, double amount) {
        member.setAvailableBalance(Math.abs(member.getAvailableBalance() + amount));
        member.setLastEarned(new Date());
        memberRepository.save(member);

        return amount;
    }

    private double calculateOverallMonthlyPv(List<Member> members) {
        return members.stream()
                .mapToDouble(m -> m.getMonthlyRightPv() + m.getMonthlyLeftPv())
                .sum();
    }

    private int calculatePvPartitions(List<Member> members, double leadershipFactor) {
        return members.stream()
                .mapToInt(m -> {
                    double weakLegPv = Math.min(m.getMonthlyLeftPv(), m.getMonthlyRightPv());
                    return weakLegPv >= leadershipFactor ? (int) (weakLegPv / leadershipFactor) : 0;
                })
                .sum();
    }

    @Async
    private void createLeadershipCommission(Member member, double amount) {
        EarningCommission earningCommission = new EarningCommission();
        earningCommission.setMemberId(member.getMemberId());
        earningCommission.setAmount(amount);
        earningCommission.setDescription(String.format("You earned %s leadership commission.", amount));
        earningCommission.setEarnedDate(LocalDateTime.now());
        earningCommission.setCommissionType(CommissionType.LEADERSHIP_COMMISSION);

        earningCommissionRepository.save(earningCommission);
    }

    @Async
    private void createCommissionRecord(Member member, double amount, String description, CommissionType commissionType) {
        EarningCommission commission = new EarningCommission();
        commission.setMemberId(member.getMemberId());
        commission.setAmount(amount);
        commission.setDescription(description);
        commission.setEarnedDate(LocalDateTime.now());
        commission.setCommissionType(commissionType);

        earningCommissionRepository.save(commission);
    }

    public void sendServiceCenterBonus(Member store, Member boughtFromStore, double pv) {
        if (store == null) {
            return;
        }
        double calculatedPv = (5.5 / 100.0) * pv;
        double value = addPvMonetaryValueToAvailableBalance(store, calculatedPv);
        createCommissionRecord(store,
                value,
                "You have earned " + value + " service center commission!",
                CommissionType.SERVICE_CENTER_COMMISSION);

        if (boughtFromStore != null) {
            double checkMarchBonus = (10 / 100.0) * value;
            addMonetaryValueToAvailableBalance(boughtFromStore, checkMarchBonus);
            createCommissionRecord(boughtFromStore,
                    checkMarchBonus,
                    "You have earned " + checkMarchBonus + " check march commission!",
                    CommissionType.CHECK_MARCH_COMMISSION);
        }
    }

    public void sendPremiumStoreBonus(Member store, double pv) {
        if (store == null) {
            return;
        }

        double calculatedPv = (8.0 / 100.0) * pv;
        double value = addPvMonetaryValueToAvailableBalance(store, calculatedPv);
        createCommissionRecord(store,
                value,
                "You have earned " + value + " premium store commission!",
                CommissionType.PREMIUM_STORE_COMMISSION);
    }

    public void sendHighestHonoraryBonus() {
        List<Member> members = memberRepository.findAll();
        List<Rank> ranks = rankRepository.findAll();
        ranks.sort(Comparator.comparingInt(Rank::getRankValue));
        Rank highestRank = ranks.get(ranks.size() - 1);

        for (Member member : members) {
            if (member.getRank() != null) {
                if (member.getRank().getRankValue() == highestRank.getRankValue()) {
                    if (member.getMonthlyLeftPv() >= member.getMonthlyRightPv()) {
                        double calculatedPv = (2 / 100.0) * member.getMonthlyRightPv();
                        double value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(member, calculatedPv);
                        memberRepository.save(member);
                        createCommissionRecord(member,
                                value,
                                "You have earned " + value + " highest honorary commission!",
                                CommissionType.HIGHEST_RANKING_COMMISSION);
                    } else {
                        double calculatedPv = (2 / 100.0) * member.getMonthlyLeftPv();
                        double value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(member, calculatedPv);
                        memberRepository.save(member);
                        createCommissionRecord(member,
                                value,
                                "You have earned " + value + " highest honorary commission!",
                                CommissionType.HIGHEST_RANKING_COMMISSION);
                    }
                }
            }
        }
    }

    public void sendLeadershipBonus() {
        double leadershipCommission = getAdminSettingValue(AdminSettings.LEADERSHIP_OVERALL_COMMISSION);
        double leadershipFactor = getAdminSettingValue(AdminSettings.LEADERSHIP_COMMISSION_FACTOR);

        List<Member> members = memberRepository.findAll();
        double overallMonthlyPv = calculateOverallMonthlyPv(members);
        double totalPvToBeShared = (leadershipCommission / 100.0) * overallMonthlyPv;

        int pvPartitions = calculatePvPartitions(members, leadershipFactor);
        if (pvPartitions == 0) return;

        double unitPvToBeShared = totalPvToBeShared / pvPartitions;
        distributeLeadershipBonus(members, leadershipFactor, unitPvToBeShared);
    }

    private void distributeLeadershipBonus(List<Member> members, double leadershipFactor, double unitPv) {
        for (Member member : members) {
            double weakLegPv = Math.min(member.getMonthlyLeftPv(), member.getMonthlyRightPv());
            if (weakLegPv >= leadershipFactor) {
                int multiple = (int) (weakLegPv / leadershipFactor);
                double leadershipBonus = unitPv * multiple;
                double value = addPvMonetaryValueToAvailableBalanceAndAwaitingWallet(member, leadershipBonus);
                memberRepository.save(member);
                createLeadershipCommission(member, value);
            }
        }
    }

    @Async
    @Scheduled(cron = DAILY_AT_MIDNIGHT, zone = AFRICA_LAGOS_TIMEZONE)
    public void runDailyBusinessOperations() {
        tryGiveEligibleMemberPromo();
    }

    @Async
    @Scheduled(cron = MONTHLY_RESET_CRON, zone = AFRICA_LAGOS_TIMEZONE)
    public void runMonthlyBusinessOperations() {
        sendLeadershipBonus();
        sendHighestHonoraryBonus();
        resetMemberAtTheEndOfTheMonth();
    }

    public void sendRank() {
        for (Member member : memberRepository.findAll()) {
            tryGivingRankToMember(member);
        }
    }
}