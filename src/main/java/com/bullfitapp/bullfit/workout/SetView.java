package com.bullfitapp.bullfit.workout;

import java.math.BigDecimal;

public record SetView(Long id, int setNumber, BigDecimal weight, int reps) {
    public String weightDisplay() { return Numbers.plain(weight); }
}