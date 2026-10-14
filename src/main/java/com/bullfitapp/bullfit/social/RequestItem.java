package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.User;

import java.time.LocalDateTime;

public record RequestItem(Long id, User user, LocalDateTime createdAt) {
}
