package com.bullfitapp.bullfit.split;

import java.time.LocalDateTime;

public record RunListItem(Long runId, String splitName, LocalDateTime startedAt,
                          LocalDateTime endedAt, long workouts) {}
