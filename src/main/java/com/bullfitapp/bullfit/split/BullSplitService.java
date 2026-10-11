package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.exercise.Exercise;
import com.bullfitapp.bullfit.exercise.ExerciseRepository;
import com.bullfitapp.bullfit.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BullSplitService {

    private static final int MAX_EXERCISES_PER_DAY = 15;

    private final BullSplitRepository splitRepository;
    private final SplitDayRepository dayRepository;
    private final SplitDayExerciseRepository sdeRepository;
    private final ExerciseRepository exerciseRepository;

    public BullSplitService(BullSplitRepository splitRepository, SplitDayRepository dayRepository,
                            SplitDayExerciseRepository sdeRepository,
                            ExerciseRepository exerciseRepository) {
        this.splitRepository = splitRepository;
        this.dayRepository = dayRepository;
        this.sdeRepository = sdeRepository;
        this.exerciseRepository = exerciseRepository;
    }

    // ----- reading -----

    @Transactional(readOnly = true)
    public long heldCount(Long userId) {
        return splitRepository.countByOwnerIdAndArchivedFalse(userId);
    }

    @Transactional(readOnly = true)
    public long publishedCount(Long userId) {
        return splitRepository.countByOwnerIdAndPublishedTrueAndArchivedFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<SplitListItem> mySplits(Long userId) {
        List<BullSplit> splits = splitRepository
                .findByOwnerIdAndCopyFalseAndArchivedFalseOrderByCreatedAtDesc(userId);
        if (splits.isEmpty()) return List.of();
        Map<Long, Long> counts = sdeRepository
                .countBySplit(splits.stream().map(BullSplit::getId).toList()).stream()
                .collect(Collectors.toMap(SplitExerciseCount::splitId, SplitExerciseCount::total));
        return splits.stream()
                .map(s -> new SplitListItem(s.getId(), s.getName(), s.getDayCount(), s.isPublished(),
                        counts.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SplitView viewOwned(Long splitId, Long userId) {
        return buildView(owned(splitId, userId));
    }

    SplitView buildView(BullSplit split) {
        List<SplitDay> days = dayRepository.findBySplitIdOrderByDayNumber(split.getId());
        List<Long> dayIds = days.stream().map(SplitDay::getId).toList();
        List<SplitDayExercise> sdes = dayIds.isEmpty() ? List.of()
                : sdeRepository.findBySplitDayIdInOrderByOrderIndex(dayIds);

        Map<Long, Exercise> exercises = exerciseRepository
                .findAllById(sdes.stream().map(SplitDayExercise::getExerciseId).toList()).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));
        Map<Long, List<SplitDayExercise>> byDay = sdes.stream()
                .collect(Collectors.groupingBy(SplitDayExercise::getSplitDayId));

        List<SplitDayView> dayViews = days.stream().map(day -> new SplitDayView(
                day.getId(), day.getDayNumber(), day.getLabel(),
                byDay.getOrDefault(day.getId(), List.of()).stream().map(sde -> {
                    Exercise ex = exercises.get(sde.getExerciseId());
                    return new SplitExerciseView(sde.getId(), ex.getId(), ex.getName(),
                            ex.getMuscleGroup(), ex.getEquipment(), ex.isCustom(),
                            sde.getTargetSets(), sde.getTargetReps());
                }).toList())).toList();

        return new SplitView(split.getId(), split.getOwnerId(), split.getName(), split.getDescription(),
                split.getDayCount(), split.isPublished(), split.isCopy(), split.getCopiedFromId(), dayViews);
    }

    // ----- creating and editing -----

    @Transactional
    public Long create(User user, SplitForm form) {
        if (splitRepository.countByOwnerIdAndArchivedFalse(user.getId()) >= SplitLimits.maxHeld(user.getRole())) {
            throw new SplitException("You've reached your limit of "
                    + SplitLimits.describe(SplitLimits.maxHeld(user.getRole()))
                    + " BullSplits. Delete one to make room.");
        }
        BullSplit split = new BullSplit();
        split.setOwnerId(user.getId());
        split.setName(form.getName().trim());
        split.setDescription(blankToNull(form.getDescription()));
        split.setDayCount(form.getDayCount());
        splitRepository.save(split);

        for (int n = 1; n <= form.getDayCount(); n++) {
            SplitDay day = new SplitDay();
            day.setSplitId(split.getId());
            day.setDayNumber(n);
            dayRepository.save(day);
        }
        return split.getId();
    }

    @Transactional
    public void updateDetails(Long splitId, Long userId, String name, String description) {
        BullSplit split = owned(splitId, userId);
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > 60) {
            throw new SplitException("Name must be 1 to 60 characters.");
        }
        String cleanDescription = blankToNull(description);
        if (cleanDescription != null && cleanDescription.length() > 300) {
            throw new SplitException("Description can be up to 300 characters.");
        }
        split.setName(cleanName);
        split.setDescription(cleanDescription);
        splitRepository.save(split);
    }

    @Transactional
    public void renameDay(Long splitId, Long userId, Long dayId, String label) {
        owned(splitId, userId);
        SplitDay day = day(dayId, splitId);
        String clean = blankToNull(label);
        if (clean != null && clean.length() > 40) {
            throw new SplitException("A day label can be up to 40 characters.");
        }
        day.setLabel(clean);
        dayRepository.save(day);
    }

    @Transactional
    public void addExercise(Long splitId, Long userId, Long dayId, Long exerciseId,
                            Integer targetSets, String targetReps) {
        BullSplit split = owned(splitId, userId);
        day(dayId, splitId);
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean visible = exercise.isActive()
                && (exercise.getOwnerId() == null || exercise.getOwnerId().equals(userId));
        if (!visible) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (split.isPublished() && exercise.isCustom()) {
            throw new SplitException("Published splits can only use master-list exercises. "
                    + "Unpublish it first, or pick a different exercise.");
        }
        if (sdeRepository.existsBySplitDayIdAndExerciseId(dayId, exerciseId)) {
            throw new SplitException("That exercise is already on this day.");
        }
        if (sdeRepository.countBySplitDayId(dayId) >= MAX_EXERCISES_PER_DAY) {
            throw new SplitException("A day can have up to " + MAX_EXERCISES_PER_DAY + " exercises.");
        }
        SplitDayExercise sde = new SplitDayExercise();
        sde.setSplitDayId(dayId);
        sde.setExerciseId(exerciseId);
        sde.setOrderIndex(sdeRepository.maxOrderIndex(dayId) + 1);
        applyTargets(sde, targetSets, targetReps);
        sdeRepository.save(sde);
    }

    @Transactional
    public void updateTargets(Long splitId, Long userId, Long dayId, Long sdeId,
                              Integer targetSets, String targetReps) {
        owned(splitId, userId);
        day(dayId, splitId);
        SplitDayExercise sde = sde(sdeId, dayId);
        applyTargets(sde, targetSets, targetReps);
        sdeRepository.save(sde);
    }

    @Transactional
    public void removeExercise(Long splitId, Long userId, Long dayId, Long sdeId) {
        owned(splitId, userId);
        day(dayId, splitId);
        sdeRepository.delete(sde(sdeId, dayId));
    }

    @Transactional
    public void moveExercise(Long splitId, Long userId, Long dayId, Long sdeId, boolean up) {
        owned(splitId, userId);
        day(dayId, splitId);
        List<SplitDayExercise> list = sdeRepository.findBySplitDayIdOrderByOrderIndex(dayId);
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(sdeId)) index = i;
        }
        if (index < 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        int other = up ? index - 1 : index + 1;
        if (other < 0 || other >= list.size()) return;
        SplitDayExercise a = list.get(index);
        SplitDayExercise b = list.get(other);
        int temp = a.getOrderIndex();
        a.setOrderIndex(b.getOrderIndex());
        b.setOrderIndex(temp);
        sdeRepository.save(a);
        sdeRepository.save(b);
    }

    @Transactional
    public void delete(Long splitId, Long userId) {
        // 6c will refuse (and archive instead) when a split has been run.
        splitRepository.delete(owned(splitId, userId)); // the database cascades days and exercises
    }

    // ----- helpers -----

    BullSplit owned(Long splitId, Long userId) {
        return splitRepository.findByIdAndOwnerId(splitId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private SplitDay day(Long dayId, Long splitId) {
        return dayRepository.findByIdAndSplitId(dayId, splitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private SplitDayExercise sde(Long sdeId, Long dayId) {
        return sdeRepository.findByIdAndSplitDayId(sdeId, dayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void applyTargets(SplitDayExercise sde, Integer sets, String reps) {
        if (sets != null && (sets < 1 || sets > 20)) {
            throw new SplitException("Target sets must be between 1 and 20.");
        }
        String cleanReps = blankToNull(reps);
        if (cleanReps != null && cleanReps.length() > 20) {
            throw new SplitException("Target reps can be up to 20 characters (for example 8-12).");
        }
        sde.setTargetSets(sets);
        sde.setTargetReps(cleanReps);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
