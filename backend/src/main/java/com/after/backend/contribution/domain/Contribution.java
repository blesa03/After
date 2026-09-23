package com.after.backend.contribution.domain;

import com.after.backend.capsule.domain.Capsule;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "capsule_id",
            nullable = false
    )
    private Capsule capsule;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "author_user_id",
            nullable = false
    )
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false,
            length = 16
    )
    private ContributionType type;

    @Column(
            name = "text_content",
            columnDefinition = "TEXT"
    )
    private String textContent;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected Contribution() {
    }

    private Contribution(
            Capsule capsule,
            User author,
            ContributionType type,
            String textContent
    ) {
        this.capsule =
                Objects.requireNonNull(
                        capsule,
                        "capsule cannot be null"
                );

        this.author =
                Objects.requireNonNull(
                        author,
                        "author cannot be null"
                );

        this.type =
                Objects.requireNonNull(
                        type,
                        "type cannot be null"
                );

        if (type != ContributionType.TEXT) {
            throw new IllegalArgumentException(
                    "Only TEXT contributions are supported"
            );
        }

        this.textContent =
                requireTextContent(
                        textContent
                );
    }

    public static Contribution text(
            Capsule capsule,
            User author,
            String textContent
    ) {
        return new Contribution(
                capsule,
                author,
                ContributionType.TEXT,
                textContent
        );
    }

    public void updateText(
            String textContent
    ) {
        if (type != ContributionType.TEXT) {
            throw new IllegalStateException(
                    "Contribution is not TEXT"
            );
        }

        this.textContent =
                requireTextContent(
                        textContent
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

    private static String requireTextContent(
            String value
    ) {
        Objects.requireNonNull(
                value,
                "textContent cannot be null"
        );

        String normalized =
                value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "textContent cannot be blank"
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

    public User getAuthor() {
        return author;
    }

    public ContributionType getType() {
        return type;
    }

    public String getTextContent() {
        return textContent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}