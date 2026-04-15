package com.mealguide.mealguide_api.global.auth.security;

import com.mealguide.mealguide_api.global.auth.domain.TokenClaims;
import com.mealguide.mealguide_api.global.auth.port.TokenProviderPort;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.login.application.port.UserQueryPort;
import com.mealguide.mealguide_api.login.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProviderPort tokenProviderPort;
    private final UserQueryPort userQueryPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
            TokenClaims tokenClaims = tokenProviderPort.parseAccessToken(accessToken);
            UserRole role = userQueryPort.findActiveRoleById(tokenClaims.userId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

            AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.authenticated(
                    tokenClaims.userId(),
                    tokenClaims.deviceId(),
                    role
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.authorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
