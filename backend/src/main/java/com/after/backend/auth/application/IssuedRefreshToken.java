package com.after.backend.auth.application;

import com.after.backend.user.domain.User;

record IssuedRefreshToken(
        User user,
        String value
) {
}