package com.bullfitapp.bullfit.exercise;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional(readOnly = true)
    public List<Exercise> search(Long userId, String q, MuscleGroup muscle, Equipment equipment) {
        String query = (q == null) ? "" : q.trim();
        return exerciseRepository.search(userId, query, muscle, equipment);
    }
}
