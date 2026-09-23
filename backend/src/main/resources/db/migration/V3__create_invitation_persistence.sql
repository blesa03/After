CREATE TABLE invitations (
    id UUID PRIMARY KEY,
    capsule_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    accepted_by_user_id UUID,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT fk_invitations_capsule
        FOREIGN KEY (capsule_id)
        REFERENCES capsules(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_invitations_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invitations_accepted_by_user
        FOREIGN KEY (accepted_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_invitations_token_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_invitations_token_hash_not_blank
        CHECK (char_length(btrim(token_hash)) > 0),

    CONSTRAINT chk_invitations_acceptance_consistent
        CHECK (
            (accepted_at IS NULL AND accepted_by_user_id IS NULL)
            OR
            (accepted_at IS NOT NULL AND accepted_by_user_id IS NOT NULL)
        ),

    CONSTRAINT chk_invitations_not_accepted_and_revoked
        CHECK (
            accepted_at IS NULL
            OR revoked_at IS NULL
        )
);

CREATE INDEX idx_invitations_capsule_id
    ON invitations(capsule_id);

CREATE INDEX idx_invitations_created_by_user_id
    ON invitations(created_by_user_id);