package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.entity.LeaveAttachment;
import com.enterprise.lms.module.leave.repository.LeaveAttachmentRepository;
import com.enterprise.lms.module.user.entity.Role;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecureFileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final LeaveAttachmentRepository leaveAttachmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public LeaveAttachment store(Long ownerUserId, MultipartFile file) throws IOException {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("OWNER_USER_ID_REQUIRED");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("FILE_REQUIRED");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("FILE_TOO_LARGE");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("UNSUPPORTED_FILE_TYPE");
        }

        userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        String originalFileName = sanitizeOriginalFileName(file.getOriginalFilename());
        String extension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
        String storedFileName = UUID.randomUUID() + extension;

        Path storageRoot = storageRoot();
        Files.createDirectories(storageRoot);
        Path storedPath = storageRoot.resolve(storedFileName);
        Files.write(storedPath, file.getBytes());

        LeaveAttachment attachment = LeaveAttachment.builder()
                .fileUuid(UUID.randomUUID().toString())
                .ownerUserId(ownerUserId)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .mimeType(contentType.toLowerCase())
                .sizeBytes(file.getSize())
                .confidential(true)
                .build();

        return leaveAttachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public boolean canDownload(Long attachmentId, Long requestingUserId, Role role) {
        if (requestingUserId == null || role == null) {
            return false;
        }

        LeaveAttachment attachment = leaveAttachmentRepository.findById(attachmentId)
                .orElse(null);
        if (attachment == null) {
            return false;
        }

        return role == Role.ROLE_HR_ADMIN || attachment.getOwnerUserId().equals(requestingUserId);
    }

    @Transactional(readOnly = true)
    public byte[] downloadBytes(Long attachmentId, Long requestingUserId, Role role) {
        if (!canDownload(attachmentId, requestingUserId, role)) {
            return null;
        }

        LeaveAttachment attachment = leaveAttachmentRepository.findById(attachmentId)
                .orElse(null);
        if (attachment == null) {
            return null;
        }

        try {
            return Files.readAllBytes(storageRoot().resolve(attachment.getStoredFileName()));
        } catch (IOException e) {
            throw new IllegalStateException("ATTACHMENT_FILE_MISSING", e);
        }
    }

    @Transactional(readOnly = true)
    public LeaveAttachment findByFileUuid(String fileUuid) {
        return leaveAttachmentRepository.findByFileUuid(fileUuid).orElse(null);
    }

    @Transactional(readOnly = true)
    public byte[] downloadByUuid(String fileUuid, Long requestingUserId, Role role) {
        LeaveAttachment attachment = findByFileUuid(fileUuid);
        if (attachment == null) {
            return null;
        }
        return downloadBytes(attachment.getId(), requestingUserId, role);
    }

    public boolean canDownloadByUuid(String fileUuid, Long requestingUserId, Role role) {
        LeaveAttachment attachment = findByFileUuid(fileUuid);
        if (attachment == null) {
            return false;
        }
        return canDownload(attachment.getId(), requestingUserId, role);
    }

    private Path storageRoot() {
        return Paths.get(System.getProperty("user.home"), "lms-attachments");
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "medical-document.pdf";
        }
        String safe = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.length() > 180 ? safe.substring(0, 180) : safe;
    }
}
