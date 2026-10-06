package com.enterprise.lms.module.leave.repository;

import com.enterprise.lms.module.leave.entity.LeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, Long> {

    Optional<LeaveAttachment> findByFileUuid(String fileUuid);
}
