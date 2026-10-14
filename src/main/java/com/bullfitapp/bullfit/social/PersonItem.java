package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.User;

public record PersonItem(User user, Relationship relationship, Long requestId) {
}
