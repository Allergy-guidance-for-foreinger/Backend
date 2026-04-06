package com.mealguide.mealguide_api.login.infrastructure.security;

import com.mealguide.mealguide_api.global.auth.domain.TokenClaims;
import com.mealguide.mealguide_api.global.auth.domain.TokenType;
import com.mealguide.mealguide_api.global.auth.port.TokenProviderPort;
import com.mealguide.mealguide_api.global.auth.security.AuthenticatedUserPrincipal;
import com.mealguide.mealguide_api.global.auth.security.JwtAuthenticationFilter;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.login.application.port.UserQueryPort;
import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserRole;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final TokenProviderPort tokenProviderPort = mock(TokenProviderPort.class);
    private final UserQueryPort userQueryPort = mock(UserQueryPort.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(tokenProviderPort, userQueryPort);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesUser() throws ServletException, IOException {
        User user = createUser(1L, "user@test.com", UserRole.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(tokenProviderPort.parseAccessToken("access-token"))
                .thenReturn(new TokenClaims(1L, null, TokenType.ACCESS));
        when(userQueryPort.findById(1L)).thenReturn(Optional.of(user));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(AuthenticatedUserPrincipal.class);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingUserForTokenThrowsServiceException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(tokenProviderPort.parseAccessToken("access-token"))
                .thenReturn(new TokenClaims(1L, null, TokenType.ACCESS));
        when(userQueryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jwtAuthenticationFilter.doFilter(request, response, filterChain))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User createUser(Long id, String email, UserRole role) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "schoolId", 100L);
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "name", "Meal Guide");
        ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }
}
