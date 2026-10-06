package com.enterprise.lms;

import com.enterprise.lms.module.user.entity.Department;
import com.enterprise.lms.module.user.entity.EmploymentStatus;
import com.enterprise.lms.module.user.entity.Role;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.DepartmentRepository;
import com.enterprise.lms.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class LmsApplicationTests {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Context loads successfully and Flyway V1 schema creates tables and seeds baseline departments")
    void contextLoadsAndFlywaySeededDepartments() {
        assertThat(departmentRepository.count()).isGreaterThanOrEqualTo(3);

        Optional<Department> engDept = departmentRepository.findByDepartmentCode("ENG");
        assertThat(engDept).isPresent();
        assertThat(engDept.get().getName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Flyway V1 schema seeds baseline users with BCrypt work factor 12 and valid hierarchy")
    void flywaySeededUsersAndHierarchy() {
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(3);

        // Verify Admin user
        Optional<User> adminOpt = userRepository.findByEmail("admin@lms.local");
        assertThat(adminOpt).isPresent();
        User admin = adminOpt.get();
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_HR_ADMIN);
        assertThat(admin.getEmploymentStatus()).isEqualTo(EmploymentStatus.PERMANENT);
        assertThat(passwordEncoder.matches("password", admin.getPasswordHash())).isTrue();

        // Verify Manager user
        Optional<User> managerOpt = userRepository.findByEmail("david.manager@lms.local");
        assertThat(managerOpt).isPresent();
        User manager = managerOpt.get();
        assertThat(manager.getRole()).isEqualTo(Role.ROLE_MANAGER);

        // Verify Employee user & reporting line — JOIN FETCH to avoid LazyInitializationException
        Optional<User> employeeOpt = userRepository.findWithManagerByEmail("sarah.engineer@lms.local");
        assertThat(employeeOpt).isPresent();
        User employee = employeeOpt.get();
        assertThat(employee.getRole()).isEqualTo(Role.ROLE_EMPLOYEE);
        assertThat(employee.getManager()).isNotNull();
        assertThat(employee.getManager().getEmail()).isEqualTo("david.manager@lms.local");
    }
}
