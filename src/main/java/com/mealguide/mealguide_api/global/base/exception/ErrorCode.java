package com.mealguide.mealguide_api.global.base.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    UNEXPECTED_SERVER_ERROR(INTERNAL_SERVER_ERROR, "COM_001", "예상하지 못한 서버 오류가 발생했습니다."),
    BINDING_ERROR(BAD_REQUEST, "COM_002", "요청 값이 올바르지 않습니다."),
    ESSENTIAL_FIELD_MISSING_ERROR(BAD_REQUEST, "COM_003", "필수 필드가 누락되었습니다."),
    INVALID_ENDPOINT(NOT_FOUND, "COM_004", "유효하지 않은 API URI입니다."),
    INVALID_HTTP_METHOD(METHOD_NOT_ALLOWED, "COM_005", "유효하지 않은 HTTP 메서드입니다."),

    // Authentication
    NEED_AUTHORIZED(UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    ACCESS_DENIED(FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    JWT_EXPIRED(UNAUTHORIZED, "AUTH_003", "JWT 토큰이 만료되었습니다."),
    JWT_INVALID(UNAUTHORIZED, "AUTH_004", "JWT 토큰이 유효하지 않습니다."),
    JWT_NOT_EXIST(UNAUTHORIZED, "AUTH_005", "JWT 토큰이 존재하지 않습니다."),
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "AUTH_006", "아이디 또는 비밀번호가 올바르지 않습니다."),
    REFRESH_TOKEN_INVALID(UNAUTHORIZED, "AUTH_007", "RefreshToken이 유효하지 않습니다."),
    BAEKJOON_HANDLE_INVALID(BAD_REQUEST, "AUTH_008", "해당 핸들은 백준에 존재하지 않습니다."),
    SOLVEDAC_COOLDOWN_ACTIVE(TOO_MANY_REQUESTS, "AUTH_009", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    GOOGLE_ID_TOKEN_INVALID(UNAUTHORIZED, "AUTH_010", "Google ID Token이 유효하지 않습니다."),
    GOOGLE_EMAIL_NOT_VERIFIED(UNAUTHORIZED, "AUTH_011", "Google 이메일이 인증되지 않았습니다."),

    // User
    USER_ALREADY_EXIST(HttpStatus.CONFLICT, "USER_001", "이미 존재하는 사용자입니다."),
    USER_NOT_FOUND(NOT_FOUND, "USER_002", "사용자를 찾을 수 없습니다."),
    NOT_CORRECT_PASSWORD(UNAUTHORIZED, "USER_003", "비밀번호가 올바르지 않습니다."),
    INVALID_EMAIL_FORMAT(BAD_REQUEST, "USER_004", "이메일 형식이 올바르지 않습니다."),
    EMAIL_SEND_ERROR(INTERNAL_SERVER_ERROR, "USER_005", "이메일 전송 중 오류가 발생했습니다."),
    INVALID_EMAIL_CODE(UNAUTHORIZED, "USER_006", "이메일 인증 코드가 올바르지 않습니다."),
    EXPIRED_EMAIL_CODE(BAD_REQUEST, "USER_007", "만료된 이메일 인증 코드입니다."),
    EMAIL_NOT_VERIFIED(UNAUTHORIZED, "USER_008", "이메일이 인증되지 않았습니다."),
    INVALID_DEPARTMENT(BAD_REQUEST, "USER_009", "유효하지 않은 학과 코드입니다."),
    PASSWORD_REQUIRED(BAD_REQUEST, "USER_011", "비밀번호는 필수 입력값입니다."),
    INVALID_PASSWORD_CONFIRM(BAD_REQUEST, "USER_012", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    DUPLICATE_BOJ_ID(CONFLICT, "USER_013", "이미 등록된 BOJ 핸들입니다."),
    DUPLICATE_STUDENT_ID(CONFLICT, "USER_014", "이미 등록된 학번입니다."),
    DUPLICATE_PHONE_NUMBER(CONFLICT, "USER_015", "이미 등록된 휴대폰 번호입니다."),
    INVALID_USER_GRADE(HttpStatus.BAD_REQUEST, "USER_016", "유효하지 않은 학년 값입니다."),
    INVALID_LANGUAGE_CODE(BAD_REQUEST, "USER_017", "유효하지 않은 언어 코드입니다."),
    INVALID_ALLERGY_CODE(BAD_REQUEST, "USER_018", "유효하지 않은 알레르기 코드입니다."),
    INVALID_RELIGIOUS_CODE(BAD_REQUEST, "USER_019", "유효하지 않은 종교 코드입니다."),
    INVALID_COUNTRY_CODE(BAD_REQUEST, "USER_020", "유효하지 않은 국가 코드입니다."),
    INVALID_SCHOOL_ID(BAD_REQUEST, "USER_021", "유효하지 않은 학교 ID입니다."),
    USER_INACTIVE(UNAUTHORIZED, "USER_022", "탈퇴한 계정입니다. 관리자에게 문의해주세요."),

    MEAL_MENU_NOT_FOUND(NOT_FOUND, "MEAL_001", "Meal menu not found."),
    REVIEW_NOT_FOUND(NOT_FOUND, "REVIEW_001", "Review not found."),
    COMMENT_NOT_FOUND(NOT_FOUND, "COMMENT_001", "Comment not found."),
    REVIEW_FORBIDDEN(FORBIDDEN, "REVIEW_002", "No permission for this review."),
    COMMENT_FORBIDDEN(FORBIDDEN, "COMMENT_002", "No permission for this comment."),
    INVALID_REVIEW_CONTENT(BAD_REQUEST, "REVIEW_003", "Invalid review content."),
    INVALID_COMMENT_CONTENT(BAD_REQUEST, "COMMENT_003", "Invalid comment content.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
