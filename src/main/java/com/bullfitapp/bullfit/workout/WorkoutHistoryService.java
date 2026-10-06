package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkoutHistoryService {

    public static final int FREE_HISTORY_DAYS = 120;
    private static final int PAGE_SIZE = 15;
    private static final LocalDateTime EPOCH = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository weRepository;
    private final WorkoutSetRepository setRepository;

    public WorkoutHistoryService(WorkoutRepository workoutRepository,
                                 WorkoutExerciseRepository weRepository,
                                 WorkoutSetRepository setRepository) {
        this.workoutRepository = workoutRepository;
        this.weRepository = weRepository;
        this.setRepository = setRepository;
    }

    /** Oldest start time this role may see, or null for no limit. */
    public LocalDateTime cutoff(Role role) {
        return role.hasPremiumAccess() ? null
                : LocalDateTime.now(ZoneOffset.UTC).minusDays(FREE_HISTORY_DAYS);
    }

    public boolean isVisible(LocalDateTime startedAt, Role role) {
        LocalDateTime cutoff = cutoff(role);
        return cutoff == null || !startedAt.isBefore(cutoff);
    }

    @Transactional(readOnly = true)
    public HistoryPage history(Long userId, Role role, int page) {
        LocalDateTime cutoff = cutoff(role);
        Page<Workout> result = workoutRepository
                .findByUserIdAndStatusAndStartedAtGreaterThanEqualOrderByStartedAtDesc(
                        userId, WorkoutStatus.COMPLETED, cutoff == null ? EPOCH : cutoff,
                        PageRequest.of(page, PAGE_SIZE));

        List<Long> ids = result.getContent().stream().map(Workout::getId).toList();
        Map<Long, List<String>> names = new HashMap<>();
        Map<Long, Long> setCounts = new HashMap<>();
        if (!ids.isEmpty()) {
            weRepository.findNames(ids).forEach(n ->
                    names.computeIfAbsent(n.workoutId(), k -> new ArrayList<>()).add(n.name()));
            setRepository.countByWorkout(ids).forEach(c -> setCounts.put(c.workoutId(), c.total()));
        }

        List<HistoryItem> items = result.getContent().stream()
                .map(w -> new HistoryItem(w.getId(), w.getStartedAt(), w.getEndedAt(),
                        names.getOrDefault(w.getId(), List.of()),
                        setCounts.getOrDefault(w.getId(), 0L)))
                .toList();

        long hidden = cutoff == null ? 0
                : workoutRepository.countByUserIdAndStatusAndStartedAtLessThan(
                userId, WorkoutStatus.COMPLETED, cutoff);

        return new HistoryPage(items, page, result.hasPrevious(), result.hasNext(), hidden);
    }
}
