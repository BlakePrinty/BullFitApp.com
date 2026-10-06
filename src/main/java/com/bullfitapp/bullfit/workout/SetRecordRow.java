package com.bullfitapp.bullfit.workout;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SetRecordRow(Long exerciseId, BigDecimal weight, Integer reps, LocalDateTime startedAt) {}
