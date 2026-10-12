package com.bullfitapp.bullfit.split;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record RunSummary(Long runId, String splitName, RunStatus status,
                         LocalDateTime startedAt, LocalDateTime endedAt,
                         long workouts, long completedExercises, long skippedExercises,
                         long totalSets, BigDecimal volume, List<RunWorkoutItem> items) {

    public long daysSpan() {
        LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now(ZoneOffset.UTC);
        return ChronoUnit.DAYS.between(startedAt.toLocalDate(), end.toLocalDate()) + 1;
    }

    public String volumeDisplay() {
        return volume.stripTrailingZeros().toPlainString();
    }
}
