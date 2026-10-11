package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.exercise.Equipment;
import com.bullfitapp.bullfit.exercise.Exercise;
import com.bullfitapp.bullfit.exercise.ExerciseService;
import com.bullfitapp.bullfit.exercise.MuscleGroup;
import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/splits")
public class BullSplitController {

    private final BullSplitService splitService;
    private final ExerciseService exerciseService;
    private final UserRepository userRepository;
    private final SplitRunService splitRunService;

    public BullSplitController(BullSplitService splitService, ExerciseService exerciseService,
                               UserRepository userRepository, SplitRunService splitRunService) {
        this.splitService = splitService;
        this.exerciseService = exerciseService;
        this.userRepository = userRepository;
        this.splitRunService = splitRunService;
    }

    private User user(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow();
    }

    private String builder(Long id) {
        return "redirect:/splits/" + id;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        User user = user(principal);
        model.addAttribute("splits", splitService.mySplits(user.getId()));
        model.addAttribute("held", splitService.heldCount(user.getId()));
        model.addAttribute("heldLimit", SplitLimits.describe(SplitLimits.maxHeld(user.getRole())));
        model.addAttribute("published", splitService.publishedCount(user.getId()));
        model.addAttribute("publishedLimit", SplitLimits.describe(SplitLimits.maxPublished(user.getRole())));
        return "split/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        SplitForm form = new SplitForm();
        form.setDayCount(4);
        model.addAttribute("form", form);
        return "split/new";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") SplitForm form, BindingResult result,
                         Principal principal) {
        if (result.hasErrors()) {
            return "split/new";
        }
        try {
            return builder(splitService.create(user(principal), form));
        } catch (SplitException e) {
            result.reject("split.invalid", e.getMessage());
            return "split/new";
        }
    }

    @GetMapping("/{id}")
    public String builderPage(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("split", splitService.viewOwned(id, user(principal).getId()));
        model.addAttribute("activeSplitId",
                splitRunService.findActive(user(principal).getId()).map(UserSplit::getSplitId).orElse(null));
        return "split/builder";
    }

    @PostMapping("/{id}/details")
    public String details(@PathVariable Long id, @RequestParam String name,
                          @RequestParam(required = false) String description,
                          Principal principal, RedirectAttributes redirect) {
        try {
            splitService.updateDetails(id, user(principal).getId(), name, description);
            redirect.addFlashAttribute("message", "Saved.");
        } catch (SplitException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return builder(id);
    }

    @PostMapping("/{id}/days/{dayId}/label")
    public String label(@PathVariable Long id, @PathVariable Long dayId,
                        @RequestParam(required = false) String label,
                        Principal principal, RedirectAttributes redirect) {
        try {
            splitService.renameDay(id, user(principal).getId(), dayId, label);
        } catch (SplitException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return builder(id) + "#day-" + dayId;
    }

    @GetMapping("/{id}/days/{dayId}/add-exercise")
    public String pickExercise(@PathVariable Long id, @PathVariable Long dayId,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) MuscleGroup muscle,
                               @RequestParam(required = false) Equipment equipment,
                               Principal principal, Model model) {
        User user = user(principal);
        SplitView split = splitService.viewOwned(id, user.getId());
        SplitDayView day = split.days().stream().filter(d -> d.id().equals(dayId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Exercise> exercises = exerciseService.search(user.getId(), q, muscle, equipment);
        if (split.published()) {
            exercises = exercises.stream().filter(e -> !e.isCustom()).toList();
        }
        model.addAttribute("split", split);
        model.addAttribute("day", day);
        model.addAttribute("exercises", exercises);
        model.addAttribute("alreadyAdded", day.exercises().stream()
                .map(SplitExerciseView::exerciseId).collect(Collectors.toSet()));
        model.addAttribute("muscleGroups", MuscleGroup.values());
        model.addAttribute("equipmentTypes", Equipment.values());
        model.addAttribute("q", q);
        model.addAttribute("selectedMuscle", muscle);
        model.addAttribute("selectedEquipment", equipment);
        return "split/pick-exercise";
    }

    @PostMapping("/{id}/days/{dayId}/exercises")
    public String addExercise(@PathVariable Long id, @PathVariable Long dayId,
                              @RequestParam Long exerciseId,
                              @RequestParam(required = false) Integer targetSets,
                              @RequestParam(required = false) String targetReps,
                              Principal principal, RedirectAttributes redirect) {
        try {
            splitService.addExercise(id, user(principal).getId(), dayId, exerciseId, targetSets, targetReps);
        } catch (SplitException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return builder(id) + "#day-" + dayId;
    }

    @PostMapping("/{id}/days/{dayId}/exercises/{sdeId}/targets")
    public String targets(@PathVariable Long id, @PathVariable Long dayId, @PathVariable Long sdeId,
                          @RequestParam(required = false) Integer targetSets,
                          @RequestParam(required = false) String targetReps,
                          Principal principal, RedirectAttributes redirect) {
        try {
            splitService.updateTargets(id, user(principal).getId(), dayId, sdeId, targetSets, targetReps);
        } catch (SplitException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return builder(id) + "#day-" + dayId;
    }

    @PostMapping("/{id}/days/{dayId}/exercises/{sdeId}/remove")
    public String remove(@PathVariable Long id, @PathVariable Long dayId, @PathVariable Long sdeId,
                         Principal principal) {
        splitService.removeExercise(id, user(principal).getId(), dayId, sdeId);
        return builder(id) + "#day-" + dayId;
    }

    @PostMapping("/{id}/days/{dayId}/exercises/{sdeId}/up")
    public String up(@PathVariable Long id, @PathVariable Long dayId, @PathVariable Long sdeId,
                     Principal principal) {
        splitService.moveExercise(id, user(principal).getId(), dayId, sdeId, true);
        return builder(id) + "#day-" + dayId;
    }

    @PostMapping("/{id}/days/{dayId}/exercises/{sdeId}/down")
    public String down(@PathVariable Long id, @PathVariable Long dayId, @PathVariable Long sdeId,
                       Principal principal) {
        splitService.moveExercise(id, user(principal).getId(), dayId, sdeId, false);
        return builder(id) + "#day-" + dayId;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        boolean archived = splitService.delete(id, user(principal).getId());
        redirect.addFlashAttribute("message", archived
                ? "That BullSplit has workout history, so it was archived instead of deleted."
                : "BullSplit deleted.");
        return "redirect:/splits";
    }

    @GetMapping("/saved")
    public String saved(Principal principal, Model model) {
        User user = user(principal);
        model.addAttribute("saved", splitService.saved(user.getId()));
        model.addAttribute("held", splitService.heldCount(user.getId()));
        model.addAttribute("heldLimit", SplitLimits.describe(SplitLimits.maxHeld(user.getRole())));
        return "split/saved";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            splitService.publish(user(principal), id);
            redirect.addFlashAttribute("message", "Published. Everyone can find it in Browse now.");
        } catch (SplitException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return builder(id);
    }

    @PostMapping("/{id}/unpublish")
    public String unpublish(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        splitService.unpublish(id, user(principal).getId());
        redirect.addFlashAttribute("message", "Unpublished. Anyone who already saved it keeps their copy.");
        return builder(id);
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, @RequestParam(required = false) Integer weeks,
                        Principal principal, RedirectAttributes redirect) {
        try {
            splitRunService.start(user(principal).getId(), id, weeks);
            redirect.addFlashAttribute("message", "BullSplit started. Your first workout is on the Workouts page.");
            return "redirect:/workouts";
        } catch (SplitException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return builder(id);
        }
    }
}
