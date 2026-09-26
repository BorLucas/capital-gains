package com.example.capitalgains.web;

import com.example.capitalgains.application.NotAuthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** The one place that knows how the logged-in user is kept in the HTTP session. */
final class Sessions {

    private static final String USER_ID = "userId";

    private Sessions() {
    }

    static void start(HttpServletRequest request, Long userId) {
        request.getSession(true).setAttribute(USER_ID, userId);
        request.changeSessionId(); // new id on login: blocks session fixation
    }

    static Long userId(HttpSession session) {
        Object id = session == null ? null : session.getAttribute(USER_ID);
        if (id instanceof Long userId) {
            return userId;
        }
        throw new NotAuthenticatedException();
    }
}
