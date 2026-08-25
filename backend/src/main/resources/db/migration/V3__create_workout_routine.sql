CREATE TABLE workout_routine (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_workout_routine_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT
);

CREATE INDEX idx_workout_routine_user_id ON workout_routine (user_id);

CREATE TABLE routine_exercise (
    id UUID PRIMARY KEY,
    workout_routine_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    position INTEGER NOT NULL,
    planned_set_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_routine_exercise_workout_routine
        FOREIGN KEY (workout_routine_id) REFERENCES workout_routine (id) ON DELETE CASCADE,
    CONSTRAINT fk_routine_exercise_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercise (id) ON DELETE RESTRICT,
    CONSTRAINT uk_routine_exercise_routine_exercise
        UNIQUE (workout_routine_id, exercise_id),
    CONSTRAINT uk_routine_exercise_routine_position
        UNIQUE (workout_routine_id, position),
    CONSTRAINT chk_routine_exercise_position
        CHECK (position >= 1),
    CONSTRAINT chk_routine_exercise_planned_set_count
        CHECK (planned_set_count >= 1)
);
