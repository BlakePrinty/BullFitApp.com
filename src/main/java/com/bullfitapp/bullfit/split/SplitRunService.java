package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.workout.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SplitRunService {

    private final UserSplitRepository userSplitRepository;
    private final BullSplitRepository splitRepository;
    private final SplitDayRepository dayRepository;
    private final SplitDayExerciseRepository sdeRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository weRepository;

    public SplitRunService(UserSplitRepository userSplitRepository, BullSplitRepository splitRepository,
                           SplitDayRepository dayRepository, SplitDayExerciseRepository sdeRepository,
                           WorkoutRepository workoutRepository, WorkoutExerciseRepository weRepository) {
        this.userSplitRepository = userSplitRepository;
        this.splitRepository = splitRepository;
        this.dayRepository = dayRepository;
        this.sdeRepository = sdeRepository;
        this.workoutRepository = workoutRepository;
        this.weRepository = weRepository;
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

        int order = 1;
        for (SplitDayExercise sde : planned) {
            WorkoutExercise we = new WorkoutExercise();
            we.setWorkoutId(workout.getId());
            we.setExerciseId(sde.getExerciseId()); // 6d swaps will substitute here
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

    private String targetText(SplitDayExercise sde) {
        if (sde.getTargetSets() != null && sde.getTargetReps() != null) {
            return sde.getTargetSets() + " x " + sde.getTargetReps();
        }
        if (sde.getTargetSets() != null) return sde.getTargetSets() + " sets";
        if (sde.getTargetReps() != null) return sde.getTargetReps() + " reps";
        return null;
    }
}
