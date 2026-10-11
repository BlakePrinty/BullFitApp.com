package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.user.Role;

import java.time.LocalDateTime;

public record BrowseRow(Long id, String name, String description, int dayCount,
                        String publisher, Role publisherRole, String publisherUsernameColor,
                        LocalDateTime createdAt, Long copyCount) {

    public String publisherColor() {
        return publisherRole == Role.DEVELOPER && publisherUsernameColor != null
                ? publisherUsernameColor : publisherRole.getColor();
    }
    public String publisherPrefix() { return publisherRole.getPrefix(); }
    public String prefixColor() { return publisherRole.getColor(); }
}
