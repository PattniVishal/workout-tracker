CREATE TABLE exercise (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    primary_muscle_group VARCHAR(50) NOT NULL,
    ${exercise_secondary_muscle_groups_column}
    category VARCHAR(50) NOT NULL,
    created_by_user_id UUID,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_exercise_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT chk_exercise_archive_custom_only
        CHECK (archived_at IS NULL OR created_by_user_id IS NOT NULL)
);

CREATE INDEX idx_exercise_created_by_user_id ON exercise (created_by_user_id);
CREATE INDEX idx_exercise_primary_muscle_group ON exercise (primary_muscle_group);

INSERT INTO exercise (id, name, primary_muscle_group, secondary_muscle_groups, category, created_by_user_id)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Bench Press', 'Chest', ARRAY['Triceps', 'Shoulders'], 'Barbell', NULL),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Squat', 'Legs', ARRAY['Glutes', 'Core'], 'Barbell', NULL),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Deadlift', 'Back', ARRAY['Legs', 'Core'], 'Barbell', NULL),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Overhead Press', 'Shoulders', ARRAY['Triceps', 'Core'], 'Barbell', NULL),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Barbell Row', 'Back', ARRAY['Biceps'], 'Barbell', NULL),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'Pull-Up', 'Back', ARRAY['Biceps'], 'Bodyweight', NULL);
