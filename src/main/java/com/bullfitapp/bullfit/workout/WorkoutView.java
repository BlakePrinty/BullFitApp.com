package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.exercise.Equipment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record WorkoutView(Long id, WorkoutStatus status, LocalDateTime startedAt,
                          LocalDateTime endedAt, List<WorkoutExerciseView> exercises) {

    public int totalSets() {
        return exercises.stream().mapToInt(e -> e.sets().size()).sum();
    }

    public BigDecimal totalVolume() {
        BigDecimal total = BigDecimal.ZERO;
        for (WorkoutExerciseView e : exercises) {
            if (e.cardio() || e.equipment() == Equipment.MACHINE_ASSISTANCE) continue;
            for (SetView s : e.sets()) {
                if (s.weight() != null) {
                    total = total.add(s.weight().multiply(BigDecimal.valueOf(s.reps())));
                }
            }
        }
        return total;
    }

    public long durationMinutes() {
        return endedAt == null ? 0 : Duration.between(startedAt, endedAt).toMinutes();
    }

    public String totalVolumeDisplay() {
        return Numbers.plain(totalVolume());
    }
}
