package com.bullfitapp.bullfit.split;

import jakarta.validation.constraints.*;

public class SplitForm {
    @NotBlank(message = "Give your BullSplit a name")
    @Size(max = 60, message = "Name can be up to 60 characters")
    private String name;

    @Size(max = 300, message = "Description can be up to 300 characters")
    private String description;

    @NotNull(message = "Choose how many days")
    @Min(value = 1, message = "A split has at least 1 day")
    @Max(value = 8, message = "A split has at most 8 days")
    private Integer dayCount;

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

    public Integer getDayCount() {
        return dayCount;
    }

    public void setDayCount(Integer dayCount) {
        this.dayCount = dayCount;
    }
}
