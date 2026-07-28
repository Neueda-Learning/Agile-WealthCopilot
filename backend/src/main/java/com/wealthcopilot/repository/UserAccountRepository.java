package com.wealthcopilot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wealthcopilot.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select user
            from UserAccount user
            where user.id = :resourceId
              and user.id = :authenticatedUserId
            """)
    Optional<UserAccount> findByIdAndAuthenticatedUserId(
            @Param("resourceId") Long resourceId,
            @Param("authenticatedUserId") Long authenticatedUserId);
}
