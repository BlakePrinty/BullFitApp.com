package com.bullfitapp.bullfit.workout;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record HistoryItem(Long id, LocalDateTime startedAt, LocalDateTime endedAt,
                          List<String> exerciseNames, long totalSets) {

    public long durationMinutes() {
        return endedAt == null ? 0 : Duration.between(startedAt, endedAt).toMinutes();
    }

    public String exercisesLabel() {
        if (exerciseNames.size() <= 3) return String.join(", ", exerciseNames);
        return String.join(", ", exerciseNames.subList(0, 3)) + " +" + (exerciseNames.size() - 3) + " more";
    }
}
