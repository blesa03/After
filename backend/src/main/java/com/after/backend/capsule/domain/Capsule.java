package com.after.backend.capsule.domain;

import com.after.backend.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "capsules")
public class Capsule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private CapsuleType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CapsuleStatus status;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sealed_at")
    private Instant sealedAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @OneToMany(
            mappedBy = "capsule",
            cascade = CascadeType.PERSIST
    )
    private Set<CapsuleMember> members = new HashSet<>();

    protected Capsule() {
    }

    private Capsule(
            String title,
            String description,
            CapsuleType type,
            Instant opensAt,
            String timezone,
            User owner
    ) {
        this.title = requireNonBlank(title, "title");
        this.description = description;

        this.type = Objects.requireNonNull(
                type,
                "type cannot be null"
        );

        this.status = CapsuleStatus.COLLECTING;

        this.opensAt = Objects.requireNonNull(
                opensAt,
                "opensAt cannot be null"
        );

        this.timezone = validateTimezone(timezone);

        Objects.requireNonNull(
                owner,
                "owner cannot be null"
        );

        addMember(owner, CapsuleMemberRole.OWNER);
    }

    public static Capsule create(
            String title,
            String description,
            CapsuleType type,
            Instant opensAt,
            String timezone,
            User owner
    ) {
        return new Capsule(
                title,
                description,
                type,
                opensAt,
                timezone,
                owner
        );
    }

    public CapsuleMember addContributor(User user) {
        return addMember(
                user,
                CapsuleMemberRole.CONTRIBUTOR
        );
    }

    private CapsuleMember addMember(
            User user,
            CapsuleMemberRole role
    ) {
        CapsuleMember member =
                new CapsuleMember(this, user, role);

        members.add(member);

        return member;
    }

    public User getOwner() {
        return members.stream()
                .filter(member ->
                        member.getRole()
                                == CapsuleMemberRole.OWNER
                )
                .map(CapsuleMember::getUser)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Capsule has no owner"
                        )
                );
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
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

    private static String validateTimezone(String timezone) {
        String value =
                requireNonBlank(timezone, "timezone");

        try {
            ZoneId.of(value);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    "timezone must be a valid zone ID",
                    exception
            );
        }

        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public CapsuleType getType() {
        return type;
    }

    public CapsuleStatus getStatus() {
        return status;
    }

    public Instant getOpensAt() {
        return opensAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSealedAt() {
        return sealedAt;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Set<CapsuleMember> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public void update(String title, String description, Instant opensAt, String timezone) {
        this.title = title;
        this.description = description;
        this.opensAt = opensAt;
        this.timezone = timezone;
    }
}