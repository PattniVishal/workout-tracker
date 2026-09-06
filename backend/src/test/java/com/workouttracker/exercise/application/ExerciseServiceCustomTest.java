package com.workouttracker.exercise.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.application.AuthService;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.common.security.UserPrincipal;
import com.workouttracker.exercise.api.CreateExerciseRequest;
import com.workouttracker.exercise.api.ExerciseListResponse;
import com.workouttracker.exercise.api.ExerciseResponse;
import com.workouttracker.exercise.domain.Exercise;
import com.workouttracker.exercise.infrastructure.ExerciseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExerciseServiceCustomTest {

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCustomPersistsOwnedExercise() {
        authenticateAs("Vishal", "custom-create@example.com");

        ExerciseResponse created = exerciseService.createCustom(new CreateExerciseRequest(
                "Smith Machine Incline Press",
                "Chest",
                List.of("Triceps"),
                "Machine"
        ));

        Exercise persisted = exerciseRepository.findById(created.id()).orElseThrow();

        assertThat(created.source()).isEqualTo("CUSTOM");
        assertThat(persisted.getCreatedByUserId()).isNotNull();
        assertThat(persisted.getArchivedAt()).isNull();
    }

    @Test
    void updateCustomUpdatesOwnedExercise() {
        UserResponse user = authenticateAs("Vishal", "custom-update@example.com");
        ExerciseResponse created = exerciseService.createCustom(new CreateExerciseRequest(
                "Cable Fly",
                "Chest",
                List.of(),
                "Cable"
        ));

        ExerciseResponse updated = exerciseService.updateCustom(
                created.id(),
                new CreateExerciseRequest(
                        "Cable Fly Updated",
                        "Chest",
                        List.of("Shoulders"),
                        "Cable"
                )
        );

        assertThat(updated.name()).isEqualTo("Cable Fly Updated");
        assertThat(updated.secondaryMuscleGroups()).containsExactly("Shoulders");
        assertThat(exerciseRepository.findById(created.id()).orElseThrow().getCreatedByUserId())
                .isEqualTo(user.id());
    }

    @Test
    void updateCustomAllowsArchivedExerciseForOwner() {
        authenticateAs("Vishal", "custom-archived-update@example.com");
        ExerciseResponse created = exerciseService.createCustom(new CreateExerciseRequest(
                "Old Custom",
                "Back",
                List.of(),
                "Machine"
        ));
        exerciseService.archiveCustom(created.id());

        ExerciseResponse updated = exerciseService.updateCustom(
                created.id(),
                new CreateExerciseRequest(
                        "Old Custom Renamed",
                        "Back",
                        List.of("Biceps"),
                        "Machine"
                )
        );

        assertThat(updated.name()).isEqualTo("Old Custom Renamed");
        assertThat(exerciseRepository.findById(created.id()).orElseThrow().isArchived()).isTrue();
    }

    @Test
    void archiveCustomSoftArchivesExercise() {
        authenticateAs("Vishal", "custom-archive@example.com");
        ExerciseResponse created = exerciseService.createCustom(new CreateExerciseRequest(
                "To Archive",
                "Legs",
                List.of(),
                "Machine"
        ));

        exerciseService.archiveCustom(created.id());

        Exercise archived = exerciseRepository.findById(created.id()).orElseThrow();
        assertThat(archived.isArchived()).isTrue();
        assertThat(exerciseRepository.count()).isGreaterThan(0);
    }

    @Test
    void archiveCustomIsIdempotent() {
        authenticateAs("Vishal", "custom-archive-idempotent@example.com");
        ExerciseResponse created = exerciseService.createCustom(new CreateExerciseRequest(
                "Archive Twice",
                "Legs",
                List.of(),
                "Machine"
        ));

        exerciseService.archiveCustom(created.id());
        Instant firstArchivedAt = exerciseRepository.findById(created.id()).orElseThrow().getArchivedAt();

        exerciseService.archiveCustom(created.id());

        assertThat(exerciseRepository.findById(created.id()).orElseThrow().getArchivedAt())
                .isEqualTo(firstArchivedAt);
    }

    @Test
    void updateCustomRejectsSystemExercise() {
        authenticateAs("Vishal", "custom-system-update@example.com");

        assertThatThrownBy(() -> exerciseService.updateCustom(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                new CreateExerciseRequest("Bench", "Chest", List.of(), "Barbell")
        )).isInstanceOf(SystemExerciseMutationException.class);
    }

    @Test
    void archiveCustomRejectsSystemExercise() {
        authenticateAs("Vishal", "custom-system-archive@example.com");

        assertThatThrownBy(() -> exerciseService.archiveCustom(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        )).isInstanceOf(SystemExerciseMutationException.class);
    }

    @Test
    void updateCustomRejectsOtherUsersExercise() {
        authenticateAs("Owner", "custom-owner@example.com");
        ExerciseResponse created = exerciseService.createCustom(new CreateExerciseRequest(
                "Private Exercise",
                "Chest",
                List.of(),
                "Machine"
        ));

        authenticateAs("Other", "custom-other@example.com");

        assertThatThrownBy(() -> exerciseService.updateCustom(
                created.id(),
                new CreateExerciseRequest("Hijacked", "Chest", List.of(), "Machine")
        )).isInstanceOf(ExerciseNotFoundException.class);
    }

    @Test
    void listPickableExcludesArchivedCustomAndOtherUsersCustom() {
        UserResponse owner = authenticateAs("Owner", "custom-list-owner@example.com");
        ExerciseResponse activeCustom = exerciseService.createCustom(new CreateExerciseRequest(
                "Owner Active",
                "Chest",
                List.of(),
                "Machine"
        ));
        ExerciseResponse archivedCustom = exerciseService.createCustom(new CreateExerciseRequest(
                "Owner Archived",
                "Back",
                List.of(),
                "Machine"
        ));
        exerciseService.archiveCustom(archivedCustom.id());

        authenticateAs("Other", "custom-list-other@example.com");
        ExerciseListResponse otherUserList = exerciseService.listPickable(null, null);

        assertThat(otherUserList.exercises())
                .extracting(ExerciseResponse::id)
                .doesNotContain(activeCustom.id(), archivedCustom.id());
        assertThat(otherUserList.exercises()).hasSize(6);

        setAuthentication(owner);
        ExerciseListResponse ownerList = exerciseService.listPickable(null, null);

        assertThat(ownerList.exercises())
                .extracting(ExerciseResponse::id)
                .contains(activeCustom.id())
                .doesNotContain(archivedCustom.id());
        assertThat(ownerList.exercises()).hasSize(7);
    }

    @Test
    void requirePickableReturnsSystemAndOwnActiveCustom() {
        UserResponse user = authenticateAs("Vishal", "custom-pickable@example.com");
        ExerciseResponse custom = exerciseService.createCustom(new CreateExerciseRequest(
                "Pickable Custom",
                "Chest",
                List.of(),
                "Machine"
        ));
        ExerciseResponse archived = exerciseService.createCustom(new CreateExerciseRequest(
                "Archived Custom",
                "Chest",
                List.of(),
                "Machine"
        ));
        exerciseService.archiveCustom(archived.id());

        Exercise system = exerciseService.requirePickable(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                user.id()
        );
        Exercise activeCustom = exerciseService.requirePickable(custom.id(), user.id());

        assertThat(system.isSystem()).isTrue();
        assertThat(activeCustom.isCustom()).isTrue();

        assertThatThrownBy(() -> exerciseService.requirePickable(archived.id(), user.id()))
                .isInstanceOf(ExerciseNotPickableException.class);
    }

    @Test
    void requireResolvableReferenceAllowsArchivedCustomForOwner() {
        UserResponse user = authenticateAs("Vishal", "custom-resolvable@example.com");
        ExerciseResponse archived = exerciseService.createCustom(new CreateExerciseRequest(
                "Resolvable Archived",
                "Chest",
                List.of(),
                "Machine"
        ));
        exerciseService.archiveCustom(archived.id());

        Exercise resolved = exerciseService.requireResolvableReference(archived.id(), user.id());

        assertThat(resolved.getName()).isEqualTo("Resolvable Archived");
        assertThat(resolved.isArchived()).isTrue();
    }

    @Test
    void requireResolvableReferenceRejectsOtherUsersCustom() {
        authenticateAs("Owner", "custom-resolvable-owner@example.com");
        ExerciseResponse created = exerciseService.createCustom(new CreateExerciseRequest(
                "Private Exercise",
                "Chest",
                List.of(),
                "Machine"
        ));

        authenticateAs("Other", "custom-resolvable-other@example.com");

        assertThatThrownBy(() -> exerciseService.requireResolvableReference(created.id(), currentUserId()))
                .isInstanceOf(ExerciseNotFoundException.class);
    }

    private UUID currentUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    private UserResponse authenticateAs(String displayName, String email) {
        UserResponse registered = authService.register(
                new RegisterRequest(displayName, email, "secret-password"));
        setAuthentication(registered);
        return registered;
    }

    private void setAuthentication(UserResponse registered) {
        UserPrincipal principal = new UserPrincipal(
                registered.id(),
                registered.displayName(),
                registered.email(),
                "hashed-password"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
