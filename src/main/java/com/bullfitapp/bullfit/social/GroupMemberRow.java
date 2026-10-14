package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.User;

import java.util.List;

public record GroupMemberRow(User user, boolean owner, boolean trainedToday, int weekCount,
        boolean metGoal, List<Boolean> days) {
}
