package com.bullfitapp.bullfit.exercise;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExerciseRequestService {

    private static final int MAX_NOTE = 500;

    private final ExerciseRequestRepository requestRepository;
    private final ExerciseRepository exerciseRepository;

    public ExerciseRequestService(ExerciseRequestRepository requestRepository,
                                  ExerciseRepository exerciseRepository) {
        this.requestRepository = requestRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional(readOnly = true)
    public List<RequestView> pending() {
        return requestRepository.findViewsByStatus(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return requestRepository.countByStatus(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<RequestView> forUser(Long userId) {
        return requestRepository.findViewsByRequester(userId);
    }

    @Transactional(readOnly = true)
    public Set<Long> pendingExerciseIds(Long userId) {
        return new HashSet<>(requestRepository.findExerciseIds(userId, RequestStatus.PENDING));
    }

    @Transactional
    public void submit(Long userId, Long exerciseId, String note) {
        Exercise exercise = exerciseRepository.findByIdAndOwnerId(exerciseId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!exercise.isActive()) {
            throw new ExerciseException(null, "Restore this exercise before submitting it.");
        }
        if (requestRepository.existsByExerciseIdAndStatus(exerciseId, RequestStatus.PENDING)) {
            throw new ExerciseException(null, "This exercise already has a pending request.");
        }
        if (exerciseRepository.existsByOwnerIdIsNullAndNameIgnoreCase(exercise.getName())) {
            throw new ExerciseException(null,
                    "An exercise with this name is already in the master list. Use that one instead.");
        }
        String cleanNote = (note == null || note.isBlank()) ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > MAX_NOTE) {
            throw new ExerciseException(null, "Note can be up to " + MAX_NOTE + " characters.");
        }
        ExerciseRequest request = new ExerciseRequest();
        request.setExerciseId(exerciseId);
        request.setRequesterId(userId);
        request.setNote(cleanNote);
        requestRepository.save(request);
    }

    @Transactional
    public void withdraw(Long requestId, Long userId) {
        ExerciseRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requirePending(request);
        request.setStatus(RequestStatus.WITHDRAWN);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    @Transactional
    public void approve(Long requestId, Long adminId) {
        ExerciseRequest request = load(requestId);
        requirePending(request);
        Exercise exercise = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (exercise.getOwnerId() == null) {
            throw new ExerciseException(null, "That exercise is already in the master list.");
        }
        if (exerciseRepository.existsByOwnerIdIsNullAndNameIgnoreCase(exercise.getName())) {
            throw new ExerciseException(null, "A master exercise named \"" + exercise.getName()
                    + "\" already exists. Reject this request and point the user to it.");
        }
        exercise.setOwnerId(null);   // promote in place
        exercise.setActive(true);
        exerciseRepository.save(exercise);

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    @Transactional
    public void reject(Long requestId, Long adminId, String reviewNote) {
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new ExerciseException(null, "Add a reason so the user knows why it was rejected.");
        }
        String cleanNote = reviewNote.trim();
        if (cleanNote.length() > MAX_NOTE) {
            throw new ExerciseException(null, "Reason can be up to " + MAX_NOTE + " characters.");
        }
        ExerciseRequest request = load(requestId);
        requirePending(request);
        request.setStatus(RequestStatus.REJECTED);
        request.setReviewNote(cleanNote);
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    private ExerciseRequest load(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requirePending(ExerciseRequest request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ExerciseException(null, "That request is no longer pending.");
        }
    }
}
