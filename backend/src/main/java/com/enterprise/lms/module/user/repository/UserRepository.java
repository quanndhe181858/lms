package com.enterprise.lms.module.user.repository;

import com.enterprise.lms.module.user.entity.Role;
import com.enterprise.lms.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    /** Eagerly joins manager and department — use when the full profile or reporting-line must be traversed. */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.manager LEFT JOIN FETCH u.department WHERE u.email = :email")
    Optional<User> findWithManagerByEmail(@Param("email") String email);

    List<User> findByManagerId(Long managerId);
    List<User> findByRole(Role role);
    List<User> findByActiveTrue();
    boolean existsByEmail(String email);
}
