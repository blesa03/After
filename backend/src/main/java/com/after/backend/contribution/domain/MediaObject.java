package com.after.backend.contribution.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "media_objects")
public class MediaObject {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contribution_id", nullable = false)
    private Contribution contribution;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MediaObjectStatus status = MediaObjectStatus.UPLOADING;

    @Column(name = "original_object_key", length = 1024)
    private String originalObjectKey;

    @Column(name = "processed_object_key", length = 1024)
    private String processedObjectKey;

    @Column(name = "thumbnail_object_key", length = 1024)
    private String thumbnailObjectKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "original_mime_type", length = 100)
    private String originalMimeType;

    @Column(name = "processed_mime_type", length = 100)
    private String processedMimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MediaObject() {}

    public MediaObject(UUID id, Contribution contribution) {
        this.id = id;
        this.contribution = contribution;
    }

    // Getters
    public UUID getId() { return id; }
    public Contribution getContribution() { return contribution; }
    public MediaObjectStatus getStatus() { return status; }
    public String getOriginalObjectKey() { return originalObjectKey; }
    public String getProcessedObjectKey() { return processedObjectKey; }
    public String getThumbnailObjectKey() { return thumbnailObjectKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getOriginalMimeType() { return originalMimeType; }
    public String getProcessedMimeType() { return processedMimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public Long getDurationMs() { return durationMs; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setStatus(MediaObjectStatus status) { this.status = status; }
    public void setOriginalObjectKey(String key) { this.originalObjectKey = key; }
    public void setProcessedObjectKey(String key) { this.processedObjectKey = key; }
    public void setThumbnailObjectKey(String key) { this.thumbnailObjectKey = key; }
    public void setOriginalFilename(String name) { this.originalFilename = name; }
    public void setOriginalMimeType(String mime) { this.originalMimeType = mime; }
    public void setProcessedMimeType(String mime) { this.processedMimeType = mime; }
    public void setSizeBytes(Long size) { this.sizeBytes = size; }
    public void setDurationMs(Long duration) { this.durationMs = duration; }
    public void setWidth(Integer width) { this.width = width; }
    public void setHeight(Integer height) { this.height = height; }
}