package com.bullfitapp.bullfit.workout;

import java.math.BigDecimal;

final class Numbers {
    private Numbers() {}

    static String plain(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }
}
