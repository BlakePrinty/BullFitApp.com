package com.bullfitapp.bullfit.split;

import java.time.LocalDateTime;

public record RunWorkoutItem(Long workoutId, LocalDateTime startedAt, String dayLabel,
                             long completed, long skipped) {}
