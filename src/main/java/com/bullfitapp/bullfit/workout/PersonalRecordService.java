package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.exercise.Equipment;
import com.bullfitapp.bullfit.exercise.Exercise;
import com.bullfitapp.bullfit.exercise.ExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonalRecordService {

    public static final int MAX_PINNED = 4;
    private static final int MAX_E1RM_REPS = 12;

    private final WorkoutSetRepository setRepository;
    private final ExerciseRepository exerciseRepository;
    private final PinnedRecordRepository pinnedRepository;
    private final WorkoutService workoutService;

    public PersonalRecordService(WorkoutSetRepository setRepository,
                                 ExerciseRepository exerciseRepository,
                                 PinnedRecordRepository pinnedRepository,
                                 WorkoutService workoutService) {
        this.setRepository = setRepository;
        this.exerciseRepository = exerciseRepository;
        this.pinnedRepository = pinnedRepository;
        this.workoutService = workoutService;
    }

    // ----- the rules -----

    /** Positive if the first set is better. Heavier wins (lighter for assisted), then more reps. */
    static int compareSets(Equipment equipment, BigDecimal w1, int r1, BigDecimal w2, int r2) {
        if (equipment.usesWeight()) {
            int cmp = nz(w1).compareTo(nz(w2));
            if (equipment == Equipment.MACHINE_ASSISTANCE) cmp = -cmp;
            if (cmp != 0) return cmp;
        }
        return Integer.compare(r1, r2);
    }

    /** Epley estimated 1RM, only for plain weighted lifts at up to 12 reps. */
    static BigDecimal estimateOneRepMax(Equipment equipment, BigDecimal weight, int reps) {
        if (weight == null || weight.signum() <= 0 || reps < 1 || reps > MAX_E1RM_REPS) return null;
        if (equipment == Equipment.BODYWEIGHT_ONLY || equipment == Equipment.BODYWEIGHT_LOADABLE
                || equipment == Equipment.MACHINE_ASSISTANCE) return null;
        if (reps == 1) return weight.setScale(1, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP));
        return weight.multiply(factor).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ----- computing records -----

    private static class Best {
        SetRecordRow row;
        BigDecimal e1rm;
    }

    private Map<Long, PersonalRecord> build(List<SetRecordRow> rows) {
        Set<Long> ids = rows.stream().map(SetRecordRow::exerciseId).collect(Collectors.toSet());
        Map<Long, Exercise> exercises = exerciseRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));

        Map<Long, Best> best = new HashMap<>();
        for (SetRecordRow row : rows) {
            Exercise ex = exercises.get(row.exerciseId());
            if (ex == null || ex.isCardio()) continue;
            Equipment eq = ex.getEquipment();
            Best b = best.computeIfAbsent(row.exerciseId(), k -> new Best());

            if (b.row == null) {
                b.row = row;
            } else {
                int cmp = compareSets(eq, row.weight(), row.reps(), b.row.weight(), b.row.reps());
                boolean earlierTie = cmp == 0 && row.startedAt().isBefore(b.row.startedAt());
                if (cmp > 0 || earlierTie) b.row = row;
            }
            BigDecimal est = estimateOneRepMax(eq, row.weight(), row.reps());
            if (est != null && (b.e1rm == null || est.compareTo(b.e1rm) > 0)) b.e1rm = est;
        }

        Map<Long, PersonalRecord> result = new HashMap<>();
        best.forEach((id, b) -> {
            Exercise ex = exercises.get(id);
            result.put(id, new PersonalRecord(id, ex.getName(), ex.getMuscleGroup(), ex.getEquipment(),
                    b.row.weight(), b.row.reps(), b.row.startedAt(), b.e1rm));
        });
        return result;
    }

    @Transactional(readOnly = true)
    public List<PersonalRecord> all(Long userId) {
        return build(setRepository.findRows(userId, WorkoutStatus.COMPLETED)).values().stream()
                .sorted(Comparator.comparing(PersonalRecord::exerciseName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // ----- pinning -----

    @Transactional(readOnly = true)
    public List<PersonalRecord> pinned(Long userId) {
        List<PinnedRecord> pins = pinnedRepository.findByUserIdOrderBySlot(userId);
        if (pins.isEmpty()) return List.of();
        List<Long> ids = pins.stream().map(PinnedRecord::getExerciseId).toList();
        Map<Long, PersonalRecord> records = build(
                setRepository.findRowsForExercises(userId, WorkoutStatus.COMPLETED, ids, -1L));
        return pins.stream()
                .map(p -> records.get(p.getExerciseId()))
                .filter(Objects::nonNull) // skip pins whose sets were all deleted
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> pinnedExerciseIds(Long userId) {
        return pinnedRepository.findByUserIdOrderBySlot(userId).stream()
                .map(PinnedRecord::getExerciseId).collect(Collectors.toSet());
    }

    @Transactional
    public void pin(Long userId, Long exerciseId) {
        List<PinnedRecord> pins = pinnedRepository.findByUserIdOrderBySlot(userId);
        if (pins.stream().anyMatch(p -> p.getExerciseId().equals(exerciseId))) {
            throw new WorkoutException("That record is already pinned.");
        }
        if (pins.size() >= MAX_PINNED) {
            throw new WorkoutException("You can pin up to " + MAX_PINNED + " records. Unpin one first.");
        }
        Map<Long, PersonalRecord> records = build(
                setRepository.findRowsForExercises(userId, WorkoutStatus.COMPLETED, List.of(exerciseId), -1L));
        if (!records.containsKey(exerciseId)) {
            throw new WorkoutException("Log that exercise in a finished workout before pinning it.");
        }
        Set<Integer> used = pins.stream().map(PinnedRecord::getSlot).collect(Collectors.toSet());
        int slot = 1;
        while (used.contains(slot)) slot++;
        PinnedRecord pin = new PinnedRecord();
        pin.setUserId(userId);
        pin.setExerciseId(exerciseId);
        pin.setSlot(slot);
        pinnedRepository.save(pin);
    }

    @Transactional
    public void unpin(Long userId, Long exerciseId) {
        pinnedRepository.deleteByUserIdAndExerciseId(userId, exerciseId);
    }

    // ----- "new record" lines for the finish summary -----

    @Transactional(readOnly = true)
    public List<String> newRecordLines(Long userId, Long workoutId) {
        WorkoutView view = workoutService.view(workoutId, userId);
        List<WorkoutExerciseView> lifts = view.exercises().stream()
                .filter(e -> !e.cardio() && !e.sets().isEmpty()).toList();
        if (lifts.isEmpty()) return List.of();

        List<Long> ids = lifts.stream().map(WorkoutExerciseView::exerciseId).toList();
        Map<Long, List<SetRecordRow>> prior = setRepository
                .findRowsForExercises(userId, WorkoutStatus.COMPLETED, ids, workoutId).stream()
                .collect(Collectors.groupingBy(SetRecordRow::exerciseId));

        List<String> lines = new ArrayList<>();
        for (WorkoutExerciseView ex : lifts) {
            List<SetRecordRow> before = prior.get(ex.exerciseId());
            if (before == null || before.isEmpty()) continue; // first time logged: nothing to beat
            Equipment eq = ex.equipment();

            SetView nowBest = ex.sets().get(0);
            for (SetView s : ex.sets()) {
                if (compareSets(eq, s.weight(), s.reps(), nowBest.weight(), nowBest.reps()) > 0) nowBest = s;
            }
            SetRecordRow beforeBest = before.get(0);
            for (SetRecordRow r : before) {
                if (compareSets(eq, r.weight(), r.reps(), beforeBest.weight(), beforeBest.reps()) > 0) beforeBest = r;
            }
            if (compareSets(eq, nowBest.weight(), nowBest.reps(),
                    beforeBest.weight(), beforeBest.reps()) > 0) {
                lines.add(ex.name() + ": new best, "
                        + PersonalRecord.describe(eq, nowBest.weight(), nowBest.reps())
                        + " (was " + PersonalRecord.describe(eq, beforeBest.weight(), beforeBest.reps()) + ")");
            }

            BigDecimal nowE = null;
            for (SetView s : ex.sets()) {
                BigDecimal e = estimateOneRepMax(eq, s.weight(), s.reps());
                if (e != null && (nowE == null || e.compareTo(nowE) > 0)) nowE = e;
            }
            BigDecimal beforeE = BigDecimal.ZERO;
            for (SetRecordRow r : before) {
                BigDecimal e = estimateOneRepMax(eq, r.weight(), r.reps());
                if (e != null && e.compareTo(beforeE) > 0) beforeE = e;
            }
            if (nowE != null && nowE.compareTo(beforeE) > 0) {
                lines.add(ex.name() + ": new estimated 1RM, " + Numbers.plain(nowE) + " lbs");
            }
        }
        return lines;
    }
}
