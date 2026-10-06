package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.exercise.Equipment;
import com.bullfitapp.bullfit.exercise.ExerciseService;
import com.bullfitapp.bullfit.exercise.MuscleGroup;
import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final WorkoutHistoryService historyService;
    private final ExerciseService exerciseService;
    private final UserRepository userRepository;

    public WorkoutController(WorkoutService workoutService, WorkoutHistoryService historyService,
                             ExerciseService exerciseService, UserRepository userRepository) {
        this.workoutService = workoutService;
        this.historyService = historyService;
        this.exerciseService = exerciseService;
        this.userRepository = userRepository;
    }

    private User user(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow();
    }

    private Long userId(Principal principal) {
        return user(principal).getId();
    }

    private String windowMessage() {
        return "That workout is older than " + WorkoutHistoryService.FREE_HISTORY_DAYS
                + " days. Premium unlocks your full history.";
    }

    /** Where to return after an action: the live workout page, or the edit page for a past workout. */
    private String back(Long id, Long userId, Long weId) {
        String base = workoutService.statusOf(id, userId) == WorkoutStatus.COMPLETED ? "/edit" : "";
        return "redirect:/workouts/" + id + base + (weId != null ? "#we-" + weId : "");
    }

    private String run(Long id, Long weId, Principal principal, RedirectAttributes redirect,
                       Consumer<Long> action) {
        Long userId = userId(principal);
        try {
            action.accept(userId);
        } catch (WorkoutException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back(id, userId, weId);
    }

    // ----- pages -----

    @GetMapping
    public String home(@RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        User user = user(principal);
        model.addAttribute("inProgressId",
                workoutService.findInProgress(user.getId()).map(Workout::getId).orElse(null));
        model.addAttribute("history", historyService.history(user.getId(), user.getRole(), Math.max(page, 0)));
        model.addAttribute("freeDays", WorkoutHistoryService.FREE_HISTORY_DAYS);
        return "workout/home";
    }

    @PostMapping("/start")
    public String start(Principal principal) {
        return "redirect:/workouts/" + workoutService.start(userId(principal)).getId();
    }

    @GetMapping("/{id}")
    public String active(@PathVariable Long id, Principal principal, Model model) {
        WorkoutView view = workoutService.view(id, userId(principal));
        if (view.status() == WorkoutStatus.COMPLETED) {
            return "redirect:/workouts/" + id + "/summary";
        }
        model.addAttribute("workout", view);
        model.addAttribute("editing", false);
        return "workout/active";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Principal principal, Model model,
                       RedirectAttributes redirect) {
        User user = user(principal);
        WorkoutView view = workoutService.view(id, user.getId());
        if (view.status() == WorkoutStatus.IN_PROGRESS) {
            return "redirect:/workouts/" + id;
        }
        if (!historyService.isVisible(view.startedAt(), user.getRole())) {
            redirect.addFlashAttribute("error", windowMessage());
            return "redirect:/workouts";
        }
        model.addAttribute("workout", view);
        model.addAttribute("editing", true);
        return "workout/active";
    }

    @GetMapping("/{id}/summary")
    public String summary(@PathVariable Long id, Principal principal, Model model,
                          RedirectAttributes redirect) {
        User user = user(principal);
        WorkoutView view = workoutService.view(id, user.getId());
        if (view.status() == WorkoutStatus.IN_PROGRESS) {
            return "redirect:/workouts/" + id;
        }
        if (!historyService.isVisible(view.startedAt(), user.getRole())) {
            redirect.addFlashAttribute("error", windowMessage());
            return "redirect:/workouts";
        }
        model.addAttribute("workout", view);
        return "workout/summary";
    }

    @GetMapping("/{id}/add-exercise")
    public String pickExercise(@PathVariable Long id,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) MuscleGroup muscle,
                               @RequestParam(required = false) Equipment equipment,
                               Principal principal, Model model, RedirectAttributes redirect) {
        User user = user(principal);
        WorkoutView view = workoutService.view(id, user.getId());
        boolean completed = view.status() == WorkoutStatus.COMPLETED;
        if (completed && !historyService.isVisible(view.startedAt(), user.getRole())) {
            redirect.addFlashAttribute("error", windowMessage());
            return "redirect:/workouts";
        }
        model.addAttribute("workoutId", id);
        model.addAttribute("backUrl", "/workouts/" + id + (completed ? "/edit" : ""));
        model.addAttribute("exercises", exerciseService.search(user.getId(), q, muscle, equipment));
        model.addAttribute("alreadyAdded", view.exercises().stream()
                .map(WorkoutExerciseView::exerciseId).collect(Collectors.toSet()));
        model.addAttribute("muscleGroups", MuscleGroup.values());
        model.addAttribute("equipmentTypes", Equipment.values());
        model.addAttribute("q", q);
        model.addAttribute("selectedMuscle", muscle);
        model.addAttribute("selectedEquipment", equipment);
        return "workout/pick-exercise";
    }

    // ----- actions -----

    @PostMapping("/{id}/exercises")
    public String addExercise(@PathVariable Long id, @RequestParam Long exerciseId,
                              Principal principal, RedirectAttributes redirect) {
        Long userId = userId(principal);
        try {
            Long weId = workoutService.addExercise(id, userId, exerciseId);
            return back(id, userId, weId);
        } catch (WorkoutException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return back(id, userId, null);
        }
    }

    @PostMapping("/{id}/exercises/{weId}/remove")
    public String removeExercise(@PathVariable Long id, @PathVariable Long weId,
                                 Principal principal, RedirectAttributes redirect) {
        return run(id, null, principal, redirect, uid -> workoutService.removeExercise(id, uid, weId));
    }

    @PostMapping("/{id}/exercises/{weId}/sets")
    public String addSet(@PathVariable Long id, @PathVariable Long weId,
                         @RequestParam(required = false) BigDecimal weight,
                         @RequestParam(required = false) Integer reps,
                         Principal principal, RedirectAttributes redirect) {
        return run(id, weId, principal, redirect,
                uid -> workoutService.addSet(id, uid, weId, weight, reps));
    }

    @PostMapping("/{id}/exercises/{weId}/sets/{setId}/delete")
    public String deleteSet(@PathVariable Long id, @PathVariable Long weId, @PathVariable Long setId,
                            Principal principal, RedirectAttributes redirect) {
        return run(id, weId, principal, redirect, uid -> workoutService.deleteSet(id, uid, setId));
    }

    @PostMapping("/{id}/exercises/{weId}/cardio")
    public String addCardio(@PathVariable Long id, @PathVariable Long weId,
                            @RequestParam(required = false) Integer minutes,
                            @RequestParam(required = false) Integer seconds,
                            @RequestParam(required = false) BigDecimal distance,
                            @RequestParam(required = false) BigDecimal speed,
                            @RequestParam(required = false) BigDecimal incline,
                            Principal principal, RedirectAttributes redirect) {
        return run(id, weId, principal, redirect, uid -> workoutService.addCardio(
                id, uid, weId, minutes, seconds, distance, speed, incline));
    }

    @PostMapping("/{id}/exercises/{weId}/cardio/{entryId}/delete")
    public String deleteCardio(@PathVariable Long id, @PathVariable Long weId, @PathVariable Long entryId,
                               Principal principal, RedirectAttributes redirect) {
        return run(id, weId, principal, redirect, uid -> workoutService.deleteCardio(id, uid, entryId));
    }

    @PostMapping("/{id}/finish")
    public String finish(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            workoutService.finish(id, userId(principal));
            return "redirect:/workouts/" + id + "/summary";
        } catch (WorkoutException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/workouts/" + id;
        }
    }

    @PostMapping("/{id}/discard")
    public String discard(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            workoutService.discard(id, userId(principal));
        } catch (WorkoutException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/workouts/" + id;
        }
        return "redirect:/workouts";
    }

    @PostMapping("/{id}/done")
    public String doneEditing(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        if (workoutService.completeEdit(id, userId(principal))) {
            redirect.addFlashAttribute("message", "That workout had nothing logged, so it was removed.");
            return "redirect:/workouts";
        }
        return "redirect:/workouts/" + id + "/summary";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        workoutService.deleteWorkout(id, userId(principal));
        redirect.addFlashAttribute("message", "Workout deleted.");
        return "redirect:/workouts";
    }
}