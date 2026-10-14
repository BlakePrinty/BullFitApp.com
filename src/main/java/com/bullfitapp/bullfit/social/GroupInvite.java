package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.User;

public record GroupInvite(Long groupId, String groupName, User invitedBy) {}
