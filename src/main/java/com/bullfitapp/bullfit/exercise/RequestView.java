package com.bullfitapp.bullfit.exercise;

import java.time.LocalDateTime;

public record RequestView(Long id, Long exerciseId, String exerciseName,
                          MuscleGroup muscleGroup, Equipment equipment,
                          String requester, RequestStatus status,
                          String note, String reviewNote, LocalDateTime createdAt) {}
