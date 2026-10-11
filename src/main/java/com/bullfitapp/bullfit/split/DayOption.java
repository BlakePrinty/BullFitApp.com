package com.bullfitapp.bullfit.split;

public record DayOption(int number, String label, long exerciseCount) {
    public String display() {
        return "Day " + number + (label == null ? "" : ": " + label) + " (" + exerciseCount + " exercises)";
    }
}
