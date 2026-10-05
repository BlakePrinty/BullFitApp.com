package com.bullfitapp.bullfit.exercise;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ExerciseService {

    public static final int MAX_CUSTOM_EXERCISES = 25;

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional(readOnly = true)
    public List<Exercise> search(Long userId, String q, MuscleGroup muscle, Equipment equipment) {
        String query = (q == null) ? "" : q.trim();
        return exerciseRepository.search(userId, query, muscle, equipment);
    }

    @Transactional(readOnly = true)
    public List<Exercise> allMaster() {
        return exerciseRepository.findByOwnerIdIsNullOrderByName();
    }

    @Transactional(readOnly = true)
    public List<Exercise> customFor(Long userId) {
        return exerciseRepository.findByOwnerIdOrderByName(userId);
    }

    @Transactional(readOnly = true)
    public long activeCustomCount(Long userId) {
        return exerciseRepository.countByOwnerIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public Exercise getMaster(Long id) {
        return exerciseRepository.findByIdAndOwnerIdIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Exercise getCustom(Long id, Long userId) {
        return exerciseRepository.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void createMaster(ExerciseForm form) {
        String name = form.getName().trim();
        if (exerciseRepository.existsByOwnerIdIsNullAndNameIgnoreCase(name)) {
            throw new ExerciseException("name", "A master exercise with that name already exists");
        }
        exerciseRepository.save(build(null, name, form));
    }

    @Transactional
    public void updateMaster(Long id, ExerciseForm form) {
        Exercise exercise = getMaster(id);
        String name = form.getName().trim();
        if (exerciseRepository.existsByOwnerIdIsNullAndNameIgnoreCaseAndIdNot(name, id)) {
            throw new ExerciseException("name", "A master exercise with that name already exists");
        }
        apply(exercise, name, form);
        exerciseRepository.save(exercise);
    }

    @Transactional
    public void setMasterActive(Long id, boolean active) {
        Exercise exercise = getMaster(id);
        exercise.setActive(active);
        exerciseRepository.save(exercise);
    }

    @Transactional
    public void createCustom(Long userId, ExerciseForm form) {
        String name = form.getName().trim();
        if (exerciseRepository.countByOwnerIdAndActiveTrue(userId) >= MAX_CUSTOM_EXERCISES) {
            throw new ExerciseException(null,
                    "You can have up to " + MAX_CUSTOM_EXERCISES
                            + " active custom exercises. Archive one to add another.");
        }
        checkCustomName(userId, name, null);
        exerciseRepository.save(build(userId, name, form));
    }

    @Transactional
    public void updateCustom(Long id, Long userId, ExerciseForm form) {
        Exercise exercise = getCustom(id, userId);
        String name = form.getName().trim();
        checkCustomName(userId, name, id);
        apply(exercise, name, form);
        exerciseRepository.save(exercise);
    }

    @Transactional
    public void setCustomActive(Long id, Long userId, boolean active) {
        Exercise exercise = getCustom(id, userId);
        if (active && !exercise.isActive()
                && exerciseRepository.countByOwnerIdAndActiveTrue(userId) >= MAX_CUSTOM_EXERCISES) {
            throw new ExerciseException(null,
                    "You already have " + MAX_CUSTOM_EXERCISES + " active custom exercises.");
        }
        exercise.setActive(active);
        exerciseRepository.save(exercise);
    }

    private void checkCustomName(Long userId, String name, Long selfId) {
        if (exerciseRepository.existsByOwnerIdIsNullAndNameIgnoreCaseAndActiveTrue(name)) {
            throw new ExerciseException("name",
                    "That exercise is already in the master list. Use that one instead.");
        }
        boolean duplicate = (selfId == null)
                ? exerciseRepository.existsByOwnerIdAndNameIgnoreCase(userId, name)
                : exerciseRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(userId, name, selfId);
        if (duplicate) {
            throw new ExerciseException("name",
                    "You already have a custom exercise with that name (it may be archived).");
        }
    }

    private Exercise build(Long ownerId, String name, ExerciseForm form) {
        Exercise exercise = new Exercise();
        exercise.setOwnerId(ownerId);
        apply(exercise, name, form);
        return exercise;
    }

    private void apply(Exercise exercise, String name, ExerciseForm form) {
        exercise.setName(name);
        exercise.setMuscleGroup(form.getMuscleGroup());
        exercise.setEquipment(form.getEquipment());
    }
}
