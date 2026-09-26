package com.example.capitalgains.web;

import com.example.capitalgains.application.NotAuthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.Instant;

/** The one place that knows how the logged-in user is kept in the HTTP session. */
final class Sessions {

    private static final String USER_ID = "userId";
    private static final String LOGGED_IN_AT = "loggedInAt";

    private Sessions() {
    }

    static void start(HttpServletRequest request, Long userId, Instant now) {
        HttpSession session = request.getSession(true);
        session.setAttribute(USER_ID, userId);
        session.setAttribute(LOGGED_IN_AT, now);
        request.changeSessionId(); // new id on login: blocks session fixation
    }

    static Long userId(HttpSession session) {
        Object id = session == null ? null : session.getAttribute(USER_ID);
        if (id instanceof Long userId) {
            return userId;
        }
        throw new NotAuthenticatedException();
    }

    static Instant loggedInAt(HttpSession session) {
        return (Instant) session.getAttribute(LOGGED_IN_AT);
    }
}
