package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.exercise.Equipment;
import com.bullfitapp.bullfit.workout.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SplitRunService {

    private final UserSplitRepository userSplitRepository;
    private final BullSplitRepository splitRepository;
    private final SplitDayRepository dayRepository;
    private final SplitDayExerciseRepository sdeRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository weRepository;
    private final SplitExerciseOverrideRepository splitExerciseOverrideRepository;
    private final WorkoutSetRepository workoutSetRepository;

    public SplitRunService(UserSplitRepository userSplitRepository, BullSplitRepository splitRepository,
                           SplitDayRepository dayRepository, SplitDayExerciseRepository sdeRepository,
                           WorkoutRepository workoutRepository, WorkoutExerciseRepository weRepository,
                           SplitExerciseOverrideRepository splitExerciseOverrideRepository, WorkoutSetRepository workoutSetRepository) {
        this.userSplitRepository = userSplitRepository;
        this.splitRepository = splitRepository;
        this.dayRepository = dayRepository;
        this.sdeRepository = sdeRepository;
        this.workoutRepository = workoutRepository;
        this.weRepository = weRepository;
        this.splitExerciseOverrideRepository = splitExerciseOverrideRepository;
        this.workoutSetRepository = workoutSetRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserSplit> findActive(Long userId) {
        return userSplitRepository.findFirstByUserIdAndStatus(userId, RunStatus.ACTIVE);
    }

    /** The user's active run for display, or null if they have none. */
    @Transactional(readOnly = true)
    public ActiveRunView activeView(Long userId) {
        UserSplit run = findActive(userId).orElse(null);
        if (run == null) return null;
        BullSplit split = splitRepository.findById(run.getSplitId()).orElseThrow();

        List<SplitDay> days = dayRepository.findBySplitIdOrderByDayNumber(split.getId());
        Map<Long, Long> perDay = days.isEmpty() ? Map.of()
                : sdeRepository.findBySplitDayIdInOrderByOrderIndex(days.stream().map(SplitDay::getId).toList())
                .stream().collect(Collectors.groupingBy(SplitDayExercise::getSplitDayId,
                        Collectors.counting()));
        List<DayOption> options = days.stream()
                .map(d -> new DayOption(d.getDayNumber(), d.getLabel(), perDay.getOrDefault(d.getId(), 0L)))
                .toList();

        String weekProgress = null;
        boolean reached = false;
        if (run.getTargetLengthDays() != null) {
            long elapsed = ChronoUnit.DAYS.between(run.getStartedAt().toLocalDate(),
                    LocalDateTime.now(ZoneOffset.UTC).toLocalDate());
            int totalWeeks = (int) Math.ceil(run.getTargetLengthDays() / 7.0);
            long week = Math.min(elapsed / 7 + 1, totalWeeks);
            weekProgress = "Week " + week + " of " + totalWeeks;
            reached = elapsed >= run.getTargetLengthDays();
        }
        return new ActiveRunView(run.getId(), split.getId(), split.getName(), split.getDayCount(),
                run.getCurrentDayNumber(), options, weekProgress, reached);
    }

    @Transactional
    public void start(Long userId, Long splitId, Integer weeks) {
        BullSplit split = splitRepository.findByIdAndOwnerId(splitId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (split.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (findActive(userId).isPresent()) {
            throw new SplitException("You already have an active BullSplit. Stop it before starting another.");
        }
        if (weeks != null && (weeks < 1 || weeks > 52)) {
            throw new SplitException("Length must be between 1 and 52 weeks.");
        }
        for (SplitDay day : dayRepository.findBySplitIdOrderByDayNumber(splitId)) {
            if (sdeRepository.countBySplitDayId(day.getId()) == 0) {
                throw new SplitException("Day " + day.getDayNumber()
                        + " has no exercises yet. Add some before starting.");
            }
        }
        UserSplit run = new UserSplit();
        run.setUserId(userId);
        run.setSplitId(splitId);
        run.setTargetLengthDays(weeks == null ? null : weeks * 7);
        run.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        userSplitRepository.save(run);
    }

    @Transactional
    public void stop(Long userId, Long runId) {
        UserSplit run = userSplitRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (run.getStatus() != RunStatus.ACTIVE) {
            throw new SplitException("That BullSplit is already stopped.");
        }
        Optional<Workout> inProgress = workoutRepository.findFirstByUserIdAndStatus(userId, WorkoutStatus.IN_PROGRESS);
        if (inProgress.isPresent() && runId.equals(inProgress.get().getUserSplitId())) {
            throw new SplitException("Finish or discard your current workout first.");
        }
        run.setStatus(RunStatus.COMPLETED);
        run.setEndedAt(LocalDateTime.now(ZoneOffset.UTC));
        userSplitRepository.save(run);
    }

    /** Starts today's workout from the active split, pre-filled. Returns the workout id. */
    @Transactional
    public Long startDayWorkout(Long userId, Integer requestedDay) {
        UserSplit run = findActive(userId)
                .orElseThrow(() -> new SplitException("You don't have an active BullSplit."));
        Optional<Workout> existing = workoutRepository.findFirstByUserIdAndStatus(userId, WorkoutStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            return existing.get().getId(); // resume instead of starting a second one
        }
        BullSplit split = splitRepository.findById(run.getSplitId()).orElseThrow();
        int dayNumber = requestedDay == null ? run.getCurrentDayNumber() : requestedDay;
        if (dayNumber < 1 || dayNumber > split.getDayCount()) {
            throw new SplitException("Pick a valid day.");
        }
        SplitDay day = dayRepository.findBySplitIdAndDayNumber(split.getId(), dayNumber).orElseThrow();
        List<SplitDayExercise> planned = sdeRepository.findBySplitDayIdOrderByOrderIndex(day.getId());
        if (planned.isEmpty()) {
            throw new SplitException("That day has no exercises. Add some to the BullSplit first.");
        }



        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        workout.setUserSplitId(run.getId());
        workout.setSplitDayId(day.getId());
        workoutRepository.save(workout);

        Map<Long, Long> overrides = splitExerciseOverrideRepository.findByUserSplitId(run.getId()).stream()
                .collect(Collectors.toMap(SplitExerciseOverride::getSplitDayExerciseId,
                        SplitExerciseOverride::getReplacementExerciseId));

        int order = 1;
        for (SplitDayExercise sde : planned) {
            Long replacement = overrides.get(sde.getId());
            WorkoutExercise we = new WorkoutExercise();
            we.setWorkoutId(workout.getId());
            we.setExerciseId(replacement != null ? replacement : sde.getExerciseId());
            we.setSwappedFromId(replacement != null ? sde.getExerciseId() : null);
            we.setSplitDayExerciseId(sde.getId());
            we.setOrderIndex(order++);
            we.setPlanned(true);
            we.setStatus(WorkoutExerciseStatus.PLANNED);
            we.setTargetText(targetText(sde));
            weRepository.save(we);
        }
        return workout.getId();
    }

    /** Called when a workout finishes: moves the run to the next day, wrapping after the last. */
    @Transactional
    public void afterWorkoutFinished(Workout workout) {
        if (workout.getUserSplitId() == null || workout.getSplitDayId() == null) return;
        UserSplit run = userSplitRepository.findById(workout.getUserSplitId()).orElse(null);
        if (run == null || run.getStatus() != RunStatus.ACTIVE) return;
        SplitDay day = dayRepository.findById(workout.getSplitDayId()).orElse(null);
        BullSplit split = splitRepository.findById(run.getSplitId()).orElse(null);
        if (day == null || split == null) return;
        run.setCurrentDayNumber(day.getDayNumber() % split.getDayCount() + 1);
        userSplitRepository.save(run);
    }

    /** "BullSplit name - Day 3: Pull" for a workout, or null for freestyle. */
    @Transactional(readOnly = true)
    public String title(Long userSplitId, Long splitDayId) {
        if (userSplitId == null || splitDayId == null) return null;
        UserSplit run = userSplitRepository.findById(userSplitId).orElse(null);
        SplitDay day = dayRepository.findById(splitDayId).orElse(null);
        if (run == null || day == null) return null;
        BullSplit split = splitRepository.findById(run.getSplitId()).orElse(null);
        if (split == null) return null;
        return split.getName() + " - Day " + day.getDayNumber()
                + (day.getLabel() == null ? "" : ": " + day.getLabel());
    }

    /** Remembers (or clears) a "for the rest of this split" swap on one run. */
    @Transactional
    public void applyOverride(Long userSplitId, Long sdeId, Long exerciseId) {
        SplitDayExercise sde = sdeRepository.findById(sdeId).orElse(null);
        if (sde == null) return;
        Optional<SplitExerciseOverride> existing =
                splitExerciseOverrideRepository.findByUserSplitIdAndSplitDayExerciseId(userSplitId, sdeId);
        if (exerciseId.equals(sde.getExerciseId())) { // swapped back to the original
            existing.ifPresent(splitExerciseOverrideRepository::delete);
            return;
        }
        SplitExerciseOverride override = existing.orElseGet(() -> {
            SplitExerciseOverride created = new SplitExerciseOverride();
            created.setUserSplitId(userSplitId);
            created.setSplitDayExerciseId(sdeId);
            return created;
        });
        override.setReplacementExerciseId(exerciseId);
        splitExerciseOverrideRepository.save(override);
    }

    @Transactional(readOnly = true)
    public List<RunListItem> completedRuns(Long userId) {
        List<UserSplit> runs = userSplitRepository.findByUserIdAndStatusOrderByEndedAtDesc(userId, RunStatus.COMPLETED);
        if (runs.isEmpty()) return List.of();
        Map<Long, BullSplit> splits = splitRepository
                .findAllById(runs.stream().map(UserSplit::getSplitId).toList()).stream()
                .collect(Collectors.toMap(BullSplit::getId, s -> s));
        Map<Long, Long> counts = workoutRepository
                .countByRuns(runs.stream().map(UserSplit::getId).toList(), WorkoutStatus.COMPLETED).stream()
                .collect(Collectors.toMap(RunWorkoutCount::runId, RunWorkoutCount::total));
        return runs.stream().map(r -> new RunListItem(r.getId(), splits.get(r.getSplitId()).getName(),
                r.getStartedAt(), r.getEndedAt(), counts.getOrDefault(r.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public RunSummary summary(Long userId, Long runId) {
        UserSplit run = userSplitRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        BullSplit split = splitRepository.findById(run.getSplitId()).orElseThrow();
        List<Workout> workouts = workoutRepository
                .findByUserSplitIdAndStatusOrderByStartedAtAsc(runId, WorkoutStatus.COMPLETED);

        long completedTotal = 0, skippedTotal = 0;
        Map<Long, Long> completedBy = new HashMap<>();
        Map<Long, Long> skippedBy = new HashMap<>();
        for (WorkoutStatusCount c : weRepository.statusCountsForRun(runId, WorkoutStatus.COMPLETED)) {
            if (c.status() == WorkoutExerciseStatus.SKIPPED) {
                skippedTotal += c.total();
                skippedBy.merge(c.workoutId(), c.total(), Long::sum);
            } else {
                completedTotal += c.total();
                completedBy.merge(c.workoutId(), c.total(), Long::sum);
            }
        }

        Map<Long, SplitDay> days = dayRepository.findAllById(workouts.stream()
                        .map(Workout::getSplitDayId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(SplitDay::getId, d -> d));

        List<RunWorkoutItem> items = workouts.stream().map(w -> {
            SplitDay day = days.get(w.getSplitDayId());
            String label = day == null ? "" : "Day " + day.getDayNumber()
                    + (day.getLabel() == null ? "" : ": " + day.getLabel());
            return new RunWorkoutItem(w.getId(), w.getStartedAt(), label,
                    completedBy.getOrDefault(w.getId(), 0L), skippedBy.getOrDefault(w.getId(), 0L));
        }).toList();

        BigDecimal volume = workoutSetRepository.volumeForRun(runId, WorkoutStatus.COMPLETED, Equipment.MACHINE_ASSISTANCE);
        return new RunSummary(runId, split.getName(), run.getStatus(), run.getStartedAt(), run.getEndedAt(),
                workouts.size(), completedTotal, skippedTotal,
                workoutSetRepository.countSetsForRun(runId, WorkoutStatus.COMPLETED),
                volume == null ? BigDecimal.ZERO : volume, items);
    }

    private String targetText(SplitDayExercise sde) {
        if (sde.getTargetSets() != null && sde.getTargetReps() != null) {
            return sde.getTargetSets() + " x " + sde.getTargetReps();
        }
        if (sde.getTargetSets() != null) return sde.getTargetSets() + " sets";
        if (sde.getTargetReps() != null) return sde.getTargetReps() + " reps";
        return null;
    }
}
