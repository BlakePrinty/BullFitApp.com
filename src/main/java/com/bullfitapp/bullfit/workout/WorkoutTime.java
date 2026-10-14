package com.bullfitapp.bullfit.workout;

import java.time.LocalDateTime;

public record WorkoutTime(Long userId, LocalDateTime startedAt) {}
