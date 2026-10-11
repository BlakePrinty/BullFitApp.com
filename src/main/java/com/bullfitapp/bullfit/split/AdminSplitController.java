package com.bullfitapp.bullfit.split;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/splits")
public class AdminSplitController {

    private final BullSplitService splitService;

    public AdminSplitController(BullSplitService splitService) {
        this.splitService = splitService;
    }

    @PostMapping("/{id}/unpublish")
    public String unpublish(@PathVariable Long id, RedirectAttributes redirect) {
        splitService.moderatorUnpublish(id);
        redirect.addFlashAttribute("message", "BullSplit unpublished and locked.");
        return "redirect:/browse";
    }
}
