CREATE TABLE iam.user_feedback (
    feedback_id       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    identity_id       uuid NOT NULL REFERENCES iam.identity(identity_id),
    category          text NOT NULL DEFAULT 'general',
    body              text NOT NULL,
    context           jsonb NOT NULL DEFAULT '{}',
    status            text NOT NULL DEFAULT 'pending',
    admin_notes       text,
    created_at        timestamptz NOT NULL DEFAULT now(),
    acknowledged_at   timestamptz,
    archived_at       timestamptz
);

CREATE INDEX ix_feedback_status     ON iam.user_feedback (status);
CREATE INDEX ix_feedback_created    ON iam.user_feedback (created_at DESC);
CREATE INDEX ix_feedback_identity   ON iam.user_feedback (identity_id);
