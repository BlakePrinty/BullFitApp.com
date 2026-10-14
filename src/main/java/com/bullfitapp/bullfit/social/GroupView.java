package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.User;

import java.time.LocalDate;
import java.util.List;

public record GroupView(Long id, String name, String description, int weeklyGoal,
                        boolean viewerIsOwner, String capacity, List<LocalDate> dates,
                        List<GroupMemberRow> rows, List<User> invited, List<User> invitable) {
    public List<GroupMemberRow> trainedToday() {
        return rows.stream().filter(GroupMemberRow::trainedToday).toList();
    }
}
