package com.topnivo.backend.model.response;

import com.topnivo.backend.model.entity.AccountDetails;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.entity.Rank;
import com.topnivo.backend.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {

    private String memberId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String image;
    private String phoneNumber;
    private String address;
    private String businessName;
    private String sponsorId;
    private String placerId;
    private String leftLegId;
    private String rightLegId;
    private String sponsorUsername;
    private String placerUsername;
    private Date registeredOn;
    private Package currentPackage;
    private AccountDetails accountDetails;
    private boolean enabled;
    private boolean canReceivePayment;
    private double totalLeftBv;
    private double totalRightBv;
    private double binaryLeftPv;
    private double binaryRightPv;
    private double monthlyLeftPv;
    private double monthlyRightPv;
    private int countNewlyRegisteredOnMonthlyWeakerLeg;
    private double availableBalance;
    private double transactionWallet;
    private double awaitingWallet;
    private double dailyBinaryEarning;
    private double cashback;
    private LocalDateTime lastActive;
    private Date lastEarned;
    private Rank rank;
    private List<Role> roles = new ArrayList<>();
}
