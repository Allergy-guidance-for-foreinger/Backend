package com.mealguide.mealguide_api.global.auth.jwt;

import com.mealguide.mealguide_api.global.auth.domain.AuthenticatedUser;
import com.mealguide.mealguide_api.global.auth.domain.TokenClaims;
import com.mealguide.mealguide_api.global.auth.domain.TokenType;
import com.mealguide.mealguide_api.global.auth.port.TokenProviderPort;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider implements TokenProviderPort {

    private static final String CLAIM_DEVICE_ID = "deviceId";
    private static final String CLAIM_TYPE = "type";

    private final JwtProperties jwtProperties;

    private SecretKey accessSecretKey;
    private SecretKey refreshSecretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void init() {
        accessSecretKey = Keys.hmacShaKeyFor(jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
        refreshSecretKey = Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(AuthenticatedUser user) {
        return generateAccessToken(user.userId());
    }

    @Override
    public String generateRefreshToken(AuthenticatedUser user) {
        return generateRefreshToken(user.userId(), user.deviceId());
    }

    @Override
    public TokenClaims parseAccessToken(String token) {
        return parseToken(token, TokenType.ACCESS, accessSecretKey);
    }

    @Override
    public TokenClaims parseRefreshToken(String token) {
        return parseToken(token, TokenType.REFRESH, refreshSecretKey);
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpirationSeconds();
    }

    @Override
    public long getRefreshTokenExpirationSeconds() {
        return jwtProperties.getRefreshTokenExpirationSeconds();
    }

    private String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TokenType.ACCESS.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(accessSecretKey)
                .compact();
    }

    private String generateRefreshToken(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new ServiceException(ErrorCode.JWT_INVALID);
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_DEVICE_ID, deviceId)
                .claim(CLAIM_TYPE, TokenType.REFRESH.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(refreshSecretKey)
                .compact();
    }

    private TokenClaims parseToken(String token, TokenType expectedType, SecretKey secretKey) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String typeClaim = claims.get(CLAIM_TYPE, String.class);
                if (typeClaim == null || typeClaim.isBlank()) {
                    throw new ServiceException(ErrorCode.JWT_INVALID);
                }
                TokenType actualType = TokenType.valueOf(typeClaim);
            if (actualType != expectedType) {
                throw new ServiceException(ErrorCode.JWT_INVALID);
            }

            String deviceId = claims.get(CLAIM_DEVICE_ID, String.class);
                if (expectedType == TokenType.REFRESH && (deviceId == null || deviceId.isBlank())) {
                    throw new ServiceException(ErrorCode.JWT_INVALID);
                }

            return new TokenClaims(
                    Long.valueOf(claims.getSubject()),
                    deviceId,
                    actualType
            );
        } catch (ExpiredJwtException exception) {
            throw new ServiceException(ErrorCode.JWT_EXPIRED);
        } catch (MalformedJwtException | UnsupportedJwtException | SecurityException | IllegalArgumentException exception) {
            throw new ServiceException(ErrorCode.JWT_INVALID);
        }
    }
}
