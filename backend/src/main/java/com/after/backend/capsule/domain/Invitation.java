package com.after.backend.capsule.domain;

import com.after.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 255
    )
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private User acceptedByUser;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected Invitation() {
    }

    public Invitation(
            Capsule capsule,
            User createdByUser,
            String tokenHash
    ) {
        this.capsule = Objects.requireNonNull(
                capsule,
                "capsule cannot be null"
        );

        this.createdByUser = Objects.requireNonNull(
                createdByUser,
                "createdByUser cannot be null"
        );

        this.tokenHash = requireNonBlank(
                tokenHash,
                "tokenHash"
        );

        validateCapsuleCanBeInvitedTo();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void accept(
            User user,
            Instant acceptedAt
    ) {
        Objects.requireNonNull(
                user,
                "user cannot be null"
        );

        Objects.requireNonNull(
                acceptedAt,
                "acceptedAt cannot be null"
        );

        validateCapsuleCanBeInvitedTo();

        if (isAccepted()) {
            throw new IllegalStateException(
                    "Invitation has already been accepted"
            );
        }

        if (isRevoked()) {
            throw new IllegalStateException(
                    "Invitation has been revoked"
            );
        }

        this.acceptedByUser = user;
        this.acceptedAt = acceptedAt;
    }

    public void revoke(Instant revokedAt) {
        Objects.requireNonNull(
                revokedAt,
                "revokedAt cannot be null"
        );

        if (isAccepted()) {
            throw new IllegalStateException(
                    "Accepted invitation cannot be revoked"
            );
        }

        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable() {
        return !isAccepted()
                && !isRevoked()
                && capsule.getType() == CapsuleType.SHARED
                && capsule.getStatus() == CapsuleStatus.COLLECTING;
    }

    private void validateCapsuleCanBeInvitedTo() {
        if (capsule.getType() != CapsuleType.SHARED) {
            throw new IllegalStateException(
                    "Invitations only apply to SHARED capsules"
            );
        }

        if (capsule.getStatus() != CapsuleStatus.COLLECTING) {
            throw new IllegalStateException(
                    "Invitations require a COLLECTING capsule"
            );
        }
    }

    private static String requireNonBlank(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null"
        );

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public Capsule getCapsule() {
        return capsule;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public User getAcceptedByUser() {
        return acceptedByUser;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}