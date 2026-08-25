CREATE TABLE workout_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    origin_routine_id UUID,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_workout_session_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_workout_session_origin_routine
        FOREIGN KEY (origin_routine_id) REFERENCES workout_routine (id) ON DELETE SET NULL,
    CONSTRAINT chk_workout_session_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT chk_workout_session_completed_at
        CHECK (
            (status = 'IN_PROGRESS' AND completed_at IS NULL)
            OR (status = 'COMPLETED' AND completed_at IS NOT NULL AND completed_at >= started_at)
        )
);

${workout_session_partial_indexes}

CREATE TABLE workout_exercise (
    id UUID PRIMARY KEY,
    workout_session_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    exercise_name VARCHAR(200) NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_workout_exercise_workout_session
        FOREIGN KEY (workout_session_id) REFERENCES workout_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_workout_exercise_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercise (id) ON DELETE RESTRICT,
    CONSTRAINT uk_workout_exercise_session_position
        UNIQUE (workout_session_id, position),
    CONSTRAINT chk_workout_exercise_position
        CHECK (position >= 1)
);

CREATE INDEX idx_workout_exercise_exercise_session
    ON workout_exercise (exercise_id, workout_session_id);

CREATE TABLE workout_set (
    id UUID PRIMARY KEY,
    workout_exercise_id UUID NOT NULL,
    set_number INTEGER NOT NULL,
    weight_kg DECIMAL(8, 2),
    repetitions INTEGER,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_workout_set_workout_exercise
        FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercise (id) ON DELETE CASCADE,
    CONSTRAINT uk_workout_set_exercise_set_number
        UNIQUE (workout_exercise_id, set_number),
    CONSTRAINT chk_workout_set_set_number
        CHECK (set_number >= 1),
    CONSTRAINT chk_workout_set_weight_kg
        CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT chk_workout_set_repetitions
        CHECK (repetitions IS NULL OR repetitions >= 0),
    CONSTRAINT chk_workout_set_completed_values
        CHECK (NOT is_completed OR (weight_kg IS NOT NULL AND repetitions IS NOT NULL))
);
