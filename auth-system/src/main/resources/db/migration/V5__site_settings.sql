CREATE TABLE IF NOT EXISTS iam.site_setting (
    setting_key   text PRIMARY KEY,
    setting_value text NOT NULL,
    updated_at    timestamptz NOT NULL DEFAULT now(),
    updated_by    uuid NULL
);

INSERT INTO iam.site_setting (setting_key, setting_value)
VALUES ('session.idle.timeout.minutes', '15');
