package com.enterprise.lms.module.leave.controller;

import com.enterprise.lms.module.leave.entity.LeaveAttachment;
import com.enterprise.lms.module.leave.service.SecureFileStorageService;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class LeaveAttachmentController {

    private final SecureFileStorageService secureFileStorageService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveAttachment> upload(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(secureFileStorageService.store(user.getId(), file));
    }

    @GetMapping("/{fileUuid}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable String fileUuid,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        LeaveAttachment attachment = secureFileStorageService.findByFileUuid(fileUuid);
        if (attachment == null || !secureFileStorageService.canDownload(attachment.getId(), user.getId(), user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DOWNLOAD_FORBIDDEN");
        }

        byte[] bytes = secureFileStorageService.downloadBytes(attachment.getId(), user.getId(), user.getRole());
        ByteArrayResource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .contentLength(bytes.length)
                .body(resource);
    }
}
