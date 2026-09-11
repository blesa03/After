CREATE TABLE capsules (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    opens_at TIMESTAMPTZ NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    sealed_at TIMESTAMPTZ,
    opened_at TIMESTAMPTZ,

    CONSTRAINT chk_capsules_title_not_blank
        CHECK (char_length(btrim(title)) > 0),

    CONSTRAINT chk_capsules_timezone_not_blank
        CHECK (char_length(btrim(timezone)) > 0),

    CONSTRAINT chk_capsules_type
        CHECK (type IN ('PERSONAL', 'SHARED')),

    CONSTRAINT chk_capsules_status
        CHECK (status IN ('COLLECTING', 'SEALED', 'OPENED'))
);

CREATE TABLE capsule_members (
    id UUID PRIMARY KEY,
    capsule_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(16) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_capsule_members_capsule
        FOREIGN KEY (capsule_id)
        REFERENCES capsules(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_capsule_members_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_capsule_members_capsule_user
        UNIQUE (capsule_id, user_id),

    CONSTRAINT chk_capsule_members_role
        CHECK (role IN ('OWNER', 'CONTRIBUTOR'))
);

CREATE UNIQUE INDEX uq_capsule_members_owner_per_capsule
    ON capsule_members(capsule_id)
    WHERE role = 'OWNER';

CREATE INDEX idx_capsule_members_user_id
    ON capsule_members(user_id);