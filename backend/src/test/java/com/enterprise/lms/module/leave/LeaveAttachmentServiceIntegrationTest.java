package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.LeaveAttachment;
import com.enterprise.lms.module.leave.repository.LeaveAttachmentRepository;
import com.enterprise.lms.module.leave.service.SecureFileStorageService;
import com.enterprise.lms.module.user.entity.Role;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class LeaveAttachmentServiceIntegrationTest {

    @Autowired
    private SecureFileStorageService secureFileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveAttachmentRepository leaveAttachmentRepository;

    @Test
    @DisplayName("Story 4.1: only the owner or HR Admin may download a confidential medical certificate")
    void uploadAndDownload_attachmentAccessIsRestrictedToOwnerAndHr() throws Exception {
        User owner = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        User manager = userRepository.findByEmail("david.manager@lms.local").orElseThrow();
        User hr = userRepository.findByEmail("admin@lms.local").orElseThrow();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "medical-certificate.pdf",
                "application/pdf",
                "certificate-content".getBytes(StandardCharsets.UTF_8)
        );

        LeaveAttachment attachment = secureFileStorageService.store(owner.getId(), file);

        assertThat(attachment.getFileUuid()).isNotBlank();
        assertThat(attachment.getStoredFileName()).isNotBlank();
        assertThat(secureFileStorageService.canDownload(attachment.getId(), owner.getId(), owner.getRole())).isTrue();
        assertThat(secureFileStorageService.canDownload(attachment.getId(), manager.getId(), manager.getRole())).isFalse();
        assertThat(secureFileStorageService.canDownload(attachment.getId(), hr.getId(), hr.getRole())).isTrue();
        assertThat(secureFileStorageService.downloadBytes(attachment.getId(), owner.getId(), owner.getRole())).isNotEmpty();
        assertThat(secureFileStorageService.downloadBytes(attachment.getId(), manager.getId(), manager.getRole())).isNull();

        leaveAttachmentRepository.delete(attachment);
    }
}
