package com.campushelp.dto;

import com.campushelp.entity.User;

public record StaffResponse(Long id, String name, String email) {
    public static StaffResponse of(User u) {
        return new StaffResponse(u.getId(), u.getName(), u.getEmail());
    }
}
