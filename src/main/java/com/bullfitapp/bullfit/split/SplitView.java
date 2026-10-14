package com.bullfitapp.bullfit.split;

import java.util.List;

public record SplitView(Long id, Long ownerId, String name, String description, int dayCount,
        boolean published, boolean locked, boolean copy, Long copiedFromId,
        List<SplitDayView> days) {
    public int totalExercises() {
        return days.stream().mapToInt(d -> d.exercises().size()).sum();
    }
}
