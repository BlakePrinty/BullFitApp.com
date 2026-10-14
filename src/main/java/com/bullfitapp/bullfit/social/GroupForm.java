package com.bullfitapp.bullfit.social;

import jakarta.validation.constraints.*;

public class GroupForm {
    @NotBlank(message = "Give your group a name")
    @Size(max = 50, message = "Name can be up to 50 characters")
    private String name;

    @Size(max = 300, message = "Description can be up to 300 characters")
    private String description;

    @NotNull(message = "Pick a weekly goal")
    @Min(value = 1, message = "The goal is at least 1 day")
    @Max(value = 7, message = "The goal is at most 7 days")
    private Integer weeklyGoal = 3;

    // getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getWeeklyGoal() {
        return weeklyGoal;
    }

    public void setWeeklyGoal(Integer weeklyGoal) {
        this.weeklyGoal = weeklyGoal;
    }
}
