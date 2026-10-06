package com.enterprise.lms.module.leave.entity;

import com.enterprise.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "leave_attachments",
        indexes = {
                @Index(name = "idx_leave_attachment_owner", columnList = "owner_user_id"),
                @Index(name = "idx_leave_attachment_uuid", columnList = "file_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_uuid", nullable = false, unique = true, length = 64)
    private String fileUuid;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "is_confidential", nullable = false)
    @Builder.Default
    private boolean confidential = true;
}
