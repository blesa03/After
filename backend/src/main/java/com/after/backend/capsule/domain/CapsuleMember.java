package com.after.backend.capsule.domain;

import com.after.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "capsule_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_capsule_members_capsule_user",
                        columnNames = {"capsule_id", "user_id"}
                )
        }
)
public class CapsuleMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private CapsuleMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected CapsuleMember() {
    }

    CapsuleMember(
            Capsule capsule,
            User user,
            CapsuleMemberRole role
    ) {
        this.capsule = Objects.requireNonNull(
                capsule,
                "capsule cannot be null"
        );

        this.user = Objects.requireNonNull(
                user,
                "user cannot be null"
        );

        this.role = Objects.requireNonNull(
                role,
                "role cannot be null"
        );
    }

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Capsule getCapsule() {
        return capsule;
    }

    public User getUser() {
        return user;
    }

    public CapsuleMemberRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}