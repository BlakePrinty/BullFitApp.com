package com.bullfitapp.bullfit.split;

import java.util.List;

public record ActiveRunView(Long runId, Long splitId, String splitName, int dayCount, int currentDay,
        List<DayOption> days, String weekProgress, boolean targetReached) {
    public String currentLabel() {
        DayOption d = days.stream().filter(o -> o.number() == currentDay).findFirst().orElse(null);
        return d == null ? "Day " + currentDay : "Day " + d.number() + (d.label() == null ? "" : ": " + d.label());
    }
}
