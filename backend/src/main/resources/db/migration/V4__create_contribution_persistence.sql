CREATE TABLE contributions (
    id UUID PRIMARY KEY,
    capsule_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    type VARCHAR(16) NOT NULL,
    text_content TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_contributions_capsule
        FOREIGN KEY (capsule_id)
        REFERENCES capsules(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_contributions_author
        FOREIGN KEY (author_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_contributions_type
        CHECK (
            type IN (
                'TEXT',
                'IMAGE',
                'AUDIO',
                'VIDEO'
            )
        ),

    CONSTRAINT chk_contributions_text_content
        CHECK (
            type <> 'TEXT'
            OR (
                text_content IS NOT NULL
                AND char_length(
                    btrim(text_content)
                ) > 0
            )
        )
);

CREATE INDEX idx_contributions_capsule_id
    ON contributions(capsule_id);

CREATE INDEX idx_contributions_author_user_id
    ON contributions(author_user_id);

CREATE INDEX idx_contributions_capsule_author
    ON contributions(
        capsule_id,
        author_user_id
    );