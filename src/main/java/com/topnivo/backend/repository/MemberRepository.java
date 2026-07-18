package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    Member findByUsername(String username);

    Member findByMemberId(String memberId);

    List<Member> findByCurrentPackageNotNull();

    List<Member> findBySponsor(Member sponsor);

    List<Member> findByRolesName(String roleName);

    Page<Member> findByRolesName(String roleName, Pageable pageable);

    List<Member> findByAvailableBalanceIsGreaterThanEqual(double amount);

    @Query(value = """
    SELECT m FROM Member m
    WHERE m.enabled = :enabled
    ORDER BY (m.totalLeftBv + m.totalRightBv) ASC
    """
    )
    Page<Member> findAllByTotalBvAndEnabledSortAsc(@Param("enabled") boolean isEnabled, Pageable pageable);


    @Query(value = """
    SELECT m FROM Member m
    WHERE m.enabled = :enabled
    ORDER BY (m.totalLeftBv + m.totalRightBv) DESC
    """
    )
    Page<Member> findAllByTotalBvAndEnabledSortDesc(@Param("enabled") boolean isEnabled, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    ORDER BY (m.totalLeftBv + m.totalRightBv) ASC
    """
    )
    Page<Member> findAllByTotalBvSortAsc(Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    ORDER BY (m.totalLeftBv + m.totalRightBv) DESC
    """
    )
    Page<Member> findAllByTotalBvSortDesc(Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    WHERE m.enabled = :enabled
    """
    )
    Page<Member> findAllByEnabled(@Param("enabled") boolean isEnabled, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE
    m.enabled = :enabled
    AND (r.name = :roleName)
    ORDER BY (m.totalLeftBv + m.totalRightBv) ASC
    """)
    Page<Member> findByRolesNameAndSortByBvAndEnabledDesc(@Param("roleName") String roleName, @Param("enabled") boolean isEnabled, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE
    m.enabled = :enabled
    AND (r.name = :roleName)
    ORDER BY (m.totalLeftBv + m.totalRightBv) ASC
    """)
    Page<Member> findByRolesNameAndSortByBvAndEnabledAsc(@Param("roleName") String roleName, @Param("enabled") boolean isEnabled, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE
    (r.name = :roleName)
    ORDER BY (m.totalLeftBv + m.totalRightBv) DESC
    """)
    Page<Member> findByRolesNameAndSortByBvDesc(@Param("roleName") String roleName, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE
    (r.name = :roleName)
    ORDER BY (m.totalLeftBv + m.totalRightBv) ASC
    """)
    Page<Member> findByRolesNameAndSortByBvAsc(@Param("roleName") String roleName, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE
    m.enabled = :enabled
    AND (r.name = :roleName)
    """)
    Page<Member> findByRolesNameAndEnabled(@Param("roleName") String roleName, @Param("enabled") boolean isEnabled, Pageable pageable);


    @Query(value = """
    SELECT m FROM Member m
    WHERE UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
    OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
    OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
    OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastName(@Param("search") String search, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    AND
    m.enabled = :enabled
    ORDER BY (m.totalLeftBv + m.totalRightBv) ASC
    
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAndEnabledAsc(@Param("search") String search, @Param("enabled") boolean isEnabled, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    ORDER BY (m.totalLeftBv + m.totalRightBv) ASC
    
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAsc(String search, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    ORDER BY (m.totalLeftBv + m.totalRightBv) DESC
    
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvDesc(String search, Pageable pageable);


    @Query(value = """
    SELECT m FROM Member m
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    AND
    m.enabled = :enabled
    ORDER BY (m.totalLeftBv + m.totalRightBv) DESC
    
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAndEnabledDesc(@Param("search") String search, @Param("enabled") boolean isEnabled, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    AND
    m.enabled = :enabled
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndIsEnabled(@Param("search") String search, @Param("enabled") boolean isEnabled, Pageable pageable);


    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
       )
    AND (r.name = :roleName)
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndRole(
            @Param("search") String search,
            @Param("roleName") String roleName,
            Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
       )
    AND (r.name = :roleName)
    AND m.enabled = :enabled
    ORDER BY (m.totalLeftBv + m.totalRightBv) ASC
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAndEnabledAsc(@Param("search") String search, @Param("roleName") String roleName, @Param("enabled") boolean isEnabled, Pageable pageable);


    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
       )
    AND (r.name = :roleName)
    AND m.enabled = :enabled
    ORDER BY (m.totalLeftBv + m.totalRightBv) DESC
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAndEnabledDesc(@Param("search") String search, @Param("roleName") String roleName, @Param("enabled") boolean isEnabled, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
       )
    AND (r.name = :roleName)
    ORDER BY (m.totalLeftBv + m.totalRightBv) DESC
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvDesc(@Param("search") String search, @Param("roleName") String roleName, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
       )
    AND (r.name = :roleName)
    ORDER BY (m.totalLeftBv + m.totalRightBv) Asc
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAsc(@Param("search") String search, @Param("roleName") String roleName, Pageable pageable);

    @Query(value = """
    SELECT m FROM Member m
    LEFT JOIN m.roles r
    WHERE (
        UPPER(m.email) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.username) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.firstName) LIKE UPPER(CONCAT('%', :search, '%'))
        OR UPPER(m.lastName) LIKE UPPER(CONCAT('%', :search, '%'))
       )
    AND (r.name = :roleName)
    AND m.enabled = :enabled
    """)
    Page<Member> findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndEnabled(@Param("search") String search, @Param("roleName") String roleName, @Param("enabled") boolean isEnabled, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.accumulatedPv), 0) FROM Member m")
    Double getTotalAccumulatedPv();

    @Query("SELECT SUM(m.availableBalance) FROM Member m")
    Double getTotalAvailableBalance();

}