package com.internetprog.shopex.config;

import com.internetprog.shopex.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security only checks {@code isEnabled()} while authenticating, so an
 * account disabled by an admin would otherwise keep working until its session
 * expired. This filter re-checks the flag on every authenticated request and
 * ends the session as soon as the account is disabled or deleted.
 *
 * Cost is one lookup by email per authenticated request, which is acceptable at
 * this project's scale.
 */
public class DisabledUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DisabledUserFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            boolean stillActive = userRepository.findByEmail(authentication.getName())
                    .map(user -> user.isEnabled())
                    .orElse(false);

            if (!stillActive) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/login?disabled");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
