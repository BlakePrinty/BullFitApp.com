package com.bullfitapp.bullfit.workout;

import java.math.BigDecimal;

public record CardioView(Long id, int durationSeconds, BigDecimal distance,
                         BigDecimal speed, BigDecimal incline) {
    public String durationDisplay() {
        return String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
    }
    public String distanceDisplay() { return Numbers.plain(distance); }
    public String speedDisplay() { return Numbers.plain(speed); }
    public String inclineDisplay() { return Numbers.plain(incline); }
}
