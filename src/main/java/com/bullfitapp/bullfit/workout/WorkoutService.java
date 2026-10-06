package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.exercise.Equipment;
import com.bullfitapp.bullfit.exercise.Exercise;
import com.bullfitapp.bullfit.exercise.ExerciseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WorkoutService {

    private static final int MAX_EXERCISES = 30;
    private static final int MAX_SETS = 30;
    private static final int MAX_CARDIO = 20;
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("2000");

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository weRepository;
    private final WorkoutSetRepository setRepository;
    private final CardioEntryRepository cardioRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutService(WorkoutRepository workoutRepository,
                          WorkoutExerciseRepository weRepository,
                          WorkoutSetRepository setRepository,
                          CardioEntryRepository cardioRepository,
                          ExerciseRepository exerciseRepository) {
        this.workoutRepository = workoutRepository;
        this.weRepository = weRepository;
        this.setRepository = setRepository;
        this.cardioRepository = cardioRepository;
        this.exerciseRepository = exerciseRepository;
    }

    // ----- lifecycle -----

    @Transactional(readOnly = true)
    public Optional<Workout> findInProgress(Long userId) {
        return workoutRepository.findFirstByUserIdAndStatus(userId, WorkoutStatus.IN_PROGRESS);
    }

    @Transactional
    public Workout start(Long userId) {
        return findInProgress(userId).orElseGet(() -> {
            Workout workout = new Workout();
            workout.setUserId(userId);
            workout.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
            return workoutRepository.save(workout);
        });
    }

    private record Classified(int logged, List<WorkoutExercise> empty) {}

    private Classified classify(Long workoutId) {
        List<WorkoutExercise> empty = new ArrayList<>();
        int logged = 0;
        for (WorkoutExercise we : weRepository.findByWorkoutIdOrderByOrderIndex(workoutId)) {
            boolean hasData = setRepository.countByWorkoutExerciseId(we.getId()) > 0
                    || cardioRepository.countByWorkoutExerciseId(we.getId()) > 0;
            if (hasData) logged++; else empty.add(we);
        }
        return new Classified(logged, empty);
    }

    @Transactional
    public void finish(Long workoutId, Long userId) {
        Workout workout = requireInProgress(workoutId, userId);
        Classified c = classify(workoutId);
        if (c.logged() == 0) {
            throw new WorkoutException(
                    "Nothing logged yet. Log at least one set or cardio entry, or discard the workout.");
        }
        weRepository.deleteAll(c.empty()); // exercises with nothing logged weren't done
        workout.setStatus(WorkoutStatus.COMPLETED);
        workout.setEndedAt(LocalDateTime.now(ZoneOffset.UTC));
        workoutRepository.save(workout);
    }

    @Transactional
    public void discard(Long workoutId, Long userId) {
        workoutRepository.delete(requireInProgress(workoutId, userId)); // DB cascades the children
    }

    // ----- exercises in a workout -----

    @Transactional
    public Long addExercise(Long workoutId, Long userId, Long exerciseId) {
        owned(workoutId, userId);
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean visible = exercise.isActive()
                && (exercise.getOwnerId() == null || exercise.getOwnerId().equals(userId));
        if (!visible) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (weRepository.existsByWorkoutIdAndExerciseId(workoutId, exerciseId)) {
            throw new WorkoutException("That exercise is already in this workout. Add more sets to it instead.");
        }
        if (weRepository.countByWorkoutId(workoutId) >= MAX_EXERCISES) {
            throw new WorkoutException("A workout can have up to " + MAX_EXERCISES + " exercises.");
        }
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkoutId(workoutId);
        we.setExerciseId(exerciseId);
        we.setOrderIndex(weRepository.maxOrderIndex(workoutId) + 1);
        weRepository.save(we);

        return weRepository.save(we).getId();
    }

    @Transactional
    public void removeExercise(Long workoutId, Long userId, Long weId) {
        owned(workoutId, userId);
        weRepository.delete(requireWorkoutExercise(weId, workoutId));
    }

    // ----- strength sets -----

    @Transactional
    public void addSet(Long workoutId, Long userId, Long weId, BigDecimal weight, Integer reps) {
        owned(workoutId, userId);
        WorkoutExercise we = requireWorkoutExercise(weId, workoutId);
        Exercise exercise = exerciseRepository.findById(we.getExerciseId()).orElseThrow();
        if (exercise.isCardio()) {
            throw new WorkoutException("Use the cardio form for this exercise.");
        }
        if (reps == null || reps < 1 || reps > 100) {
            throw new WorkoutException("Reps must be between 1 and 100.");
        }
        Equipment equipment = exercise.getEquipment();
        BigDecimal w = equipment.usesWeight() ? weight : null;
        if (equipment == Equipment.BODYWEIGHT_LOADABLE && w == null) {
            w = BigDecimal.ZERO; // no added weight
        }
        if (equipment.weightRequired() && w == null) {
            throw new WorkoutException("Enter a weight.");
        }
        if (w != null && (w.signum() < 0 || w.compareTo(MAX_WEIGHT) > 0)) {
            throw new WorkoutException("Weight must be between 0 and " + MAX_WEIGHT + " lbs.");
        }
        long count = setRepository.countByWorkoutExerciseId(weId);
        if (count >= MAX_SETS) {
            throw new WorkoutException("That's the maximum of " + MAX_SETS + " sets for one exercise.");
        }
        WorkoutSet set = new WorkoutSet();
        set.setWorkoutExerciseId(weId);
        set.setSetNumber((int) count + 1);
        set.setWeight(w);
        set.setReps(reps);
        setRepository.save(set);
    }

    @Transactional
    public void deleteSet(Long workoutId, Long userId, Long setId) {
        owned(workoutId, userId);
        WorkoutSet set = setRepository.findById(setId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireWorkoutExercise(set.getWorkoutExerciseId(), workoutId); // set must belong to this workout
        Long weId = set.getWorkoutExerciseId();
        setRepository.delete(set);
        setRepository.flush();
        int number = 1;
        for (WorkoutSet remaining : setRepository.findByWorkoutExerciseIdOrderBySetNumber(weId)) {
            remaining.setSetNumber(number++);
        }
    }

    // ----- cardio -----

    @Transactional
    public void addCardio(Long workoutId, Long userId, Long weId,
                          Integer minutes, Integer seconds,
                          BigDecimal distance, BigDecimal speed, BigDecimal incline) {
        owned(workoutId, userId);
        WorkoutExercise we = requireWorkoutExercise(weId, workoutId);
        Exercise exercise = exerciseRepository.findById(we.getExerciseId()).orElseThrow();
        if (!exercise.isCardio()) {
            throw new WorkoutException("Use the sets form for this exercise.");
        }
        long total = (minutes == null ? 0 : minutes) * 60L + (seconds == null ? 0 : seconds);
        if (total < 1 || total > 86_400) {
            throw new WorkoutException("Enter a duration between 1 second and 24 hours.");
        }
        checkRange(distance, 1000, "Distance");
        checkRange(speed, 100, "Speed");
        checkRange(incline, 100, "Incline");
        if (cardioRepository.countByWorkoutExerciseId(weId) >= MAX_CARDIO) {
            throw new WorkoutException("That's the maximum of " + MAX_CARDIO + " cardio entries for one exercise.");
        }
        CardioEntry entry = new CardioEntry();
        entry.setWorkoutExerciseId(weId);
        entry.setDurationSeconds((int) total);
        entry.setDistance(distance);
        entry.setSpeed(speed);
        entry.setIncline(incline);
        cardioRepository.save(entry);
    }

    @Transactional
    public void deleteCardio(Long workoutId, Long userId, Long entryId) {
        owned(workoutId, userId);
        CardioEntry entry = cardioRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireWorkoutExercise(entry.getWorkoutExerciseId(), workoutId);
        cardioRepository.delete(entry);
    }

    // ----- reading -----

    @Transactional(readOnly = true)
    public WorkoutView view(Long workoutId, Long userId) {
        Workout workout = owned(workoutId, userId);
        List<WorkoutExercise> wes = weRepository.findByWorkoutIdOrderByOrderIndex(workoutId);
        List<Long> weIds = wes.stream().map(WorkoutExercise::getId).toList();

        Map<Long, Exercise> exercises = exerciseRepository
                .findAllById(wes.stream().map(WorkoutExercise::getExerciseId).toList())
                .stream().collect(Collectors.toMap(Exercise::getId, e -> e));

        Map<Long, List<WorkoutSet>> sets = weIds.isEmpty() ? Map.of()
                : setRepository.findByWorkoutExerciseIdInOrderBySetNumber(weIds).stream()
                .collect(Collectors.groupingBy(WorkoutSet::getWorkoutExerciseId));
        Map<Long, List<CardioEntry>> cardio = weIds.isEmpty() ? Map.of()
                : cardioRepository.findByWorkoutExerciseIdInOrderById(weIds).stream()
                .collect(Collectors.groupingBy(CardioEntry::getWorkoutExerciseId));

        List<WorkoutExerciseView> views = new ArrayList<>();
        for (WorkoutExercise we : wes) {
            Exercise ex = exercises.get(we.getExerciseId());
            views.add(new WorkoutExerciseView(
                    we.getId(), ex.getId(), ex.getName(), ex.getMuscleGroup(), ex.getEquipment(),
                    ex.isCardio(),
                    sets.getOrDefault(we.getId(), List.of()).stream()
                            .map(s -> new SetView(s.getId(), s.getSetNumber(), s.getWeight(), s.getReps())).toList(),
                    cardio.getOrDefault(we.getId(), List.of()).stream()
                            .map(c -> new CardioView(c.getId(), c.getDurationSeconds(),
                                    c.getDistance(), c.getSpeed(), c.getIncline())).toList()));
        }
        return new WorkoutView(workout.getId(), workout.getStatus(),
                workout.getStartedAt(), workout.getEndedAt(), views);
    }

    // ----- helpers -----

    private Workout owned(Long workoutId, Long userId) {
        return workoutRepository.findByIdAndUserId(workoutId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Workout requireInProgress(Long workoutId, Long userId) {
        Workout workout = owned(workoutId, userId);
        if (workout.getStatus() != WorkoutStatus.IN_PROGRESS) {
            throw new WorkoutException("This workout is already finished.");
        }
        return workout;
    }

    private WorkoutExercise requireWorkoutExercise(Long weId, Long workoutId) {
        return weRepository.findByIdAndWorkoutId(weId, workoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void checkRange(BigDecimal value, int max, String label) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0)) {
            throw new WorkoutException(label + " must be between 0 and " + max + ".");
        }
    }

    @Transactional(readOnly = true)
    public WorkoutStatus statusOf(Long workoutId, Long userId) {
        return owned(workoutId, userId).getStatus();
    }

    @Transactional
    public void deleteWorkout(Long workoutId, Long userId) {
        workoutRepository.delete(owned(workoutId, userId)); // DB cascades the children
    }

    /** Called when the user finishes editing. Returns true if nothing was left and the workout was deleted. */
    @Transactional
    public boolean completeEdit(Long workoutId, Long userId) {
        Workout workout = owned(workoutId, userId);
        Classified c = classify(workoutId);
        if (c.logged() == 0) {
            workoutRepository.delete(workout);
            return true;
        }
        weRepository.deleteAll(c.empty());
        return false;
    }
}
