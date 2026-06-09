package com.topnivo.backend.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(unique = true)
    private String memberId;
    private String firstName;
    private String lastName;
    private String email;
    @Column(unique = true)
    private String username;
    private String password;
    private String businessName;
    @Column(columnDefinition = "TEXT")
    private String image;
    private String address;
    private String phoneNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    private Member sponsor;
    @ManyToOne(fetch = FetchType.LAZY)
    private Member placer;
    @OneToOne(fetch = FetchType.LAZY)
    private Member leftLeg;
    @OneToOne(fetch = FetchType.LAZY)
    private Member rightLeg;
    @Temporal(TemporalType.DATE)
    @CreationTimestamp
    private Date registeredOn;
    @OneToOne(fetch = FetchType.EAGER)
    private Cart cart;
    @OneToMany(fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
    @OneToMany(fetch = FetchType.LAZY)
    private List<EarningCommission> allBonuses = new ArrayList<>();
    @ManyToOne(fetch = FetchType.EAGER)
    private Package currentPackage;
    private boolean enabled;
    private boolean canReceivePayment;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AccountDetails accountDetails;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double totalLeftBv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double totalRightBv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double binaryLeftPv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double binaryRightPv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double monthlyLeftPv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double monthlyRightPv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double monthlySalesPv;
    private int countNewlyRegisteredOnMonthlyWeakerLeg;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double availableBalance;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double awaitingWallet;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double dailyBinaryEarning;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double cashback;
    private LocalDateTime lastActive;
    @Temporal(TemporalType.DATE)
    private Date lastEarned;
    @ManyToOne(fetch = FetchType.EAGER)
    private Rank rank;
    @ElementCollection
    @CollectionTable(name = "member_linked_accounts", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "usernames")
    private List<String> accountListUsernames = new ArrayList<>();
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<StoreProduct> storeProducts = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return id == member.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", memberId='" + memberId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", registeredOn=" + registeredOn +
                ", enabled=" + enabled +
                ", totalLeftBv=" + totalLeftBv +
                ", totalRightBv=" + totalRightBv +
                ", availableBalance=" + availableBalance +
                ", accountListUsernames=" + accountListUsernames +
                ", roles=" + roles +
                '}';
    }
}
