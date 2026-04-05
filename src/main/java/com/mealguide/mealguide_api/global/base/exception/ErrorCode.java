package com.mealguide.mealguide_api.global.base.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    UNEXPECTED_SERVER_ERROR(INTERNAL_SERVER_ERROR,"COM_001","예상치 못한 서버 오류가 발생했습니다."),
    BINDING_ERROR(BAD_REQUEST,"COM_002","요청 데이터 변환 과정에서 오류가 발생했습니다."),
    ESSENTIAL_FIELD_MISSING_ERROR(BAD_REQUEST , "COM_003","필수 필드를 누락했습니다."),
    INVALID_ENDPOINT(NOT_FOUND, "COM_004", "잘못된 API URI로 요청했습니다."),
    INVALID_HTTP_METHOD(METHOD_NOT_ALLOWED, "COM_005","잘못된 HTTP 메서드로 요청했습니다."),

    // Authentication
    NEED_AUTHORIZED(UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    ACCESS_DENIED(FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    JWT_EXPIRED(UNAUTHORIZED, "AUTH_003", "인증 정보가 만료되었습니다."),
    JWT_INVALID(UNAUTHORIZED, "AUTH_004", "인증 정보가 잘못되었습니다."),
    JWT_NOT_EXIST(UNAUTHORIZED, "AUTH_005", "인증 정보가 존재하지 않습니다."),
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED,"AUTH_006","아이디 또는 비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_INVALID(UNAUTHORIZED, "AUTH_007", "RefreshToken이 존재하지 않습니다."),
    BAEKJOON_HANDLE_INVALID(BAD_REQUEST, "AUTH_008", "유효하지 않은 백준 아이디입니다."),
    SOLVEDAC_COOLDOWN_ACTIVE(TOO_MANY_REQUESTS, "AUTH_009", "요청이 많습니다. 잠시 후 다시 시도해주세요."),

    //User
    USER_ALREADY_EXIST(HttpStatus.CONFLICT,"USER_001","이미 존재하는 회원입니다."),
    USER_NOT_FOUND(NOT_FOUND,"USER_002","존재하지 않는 유저입니다."),
    NOT_CORRECT_PASSWORD(UNAUTHORIZED,"USER_003","기존 비밀번호가 일치하지 않습니다."),
    INVALID_EMAIL_FORMAT(BAD_REQUEST,"USER_004","이메일 형식이 올바르지 않습니다."),
    EMAIL_SEND_ERROR(INTERNAL_SERVER_ERROR,"USER_005","알 수 없는 오류로 인해 이메일 전송에 실패하였습니다."),
    INVALID_EMAIL_CODE(UNAUTHORIZED,"USER_006","인증번호가 일치하지 않습니다."),
    EXPIRED_EMAIL_CODE(BAD_REQUEST,"USER_007","만료된 인증번호입니다."),
    EMAIL_NOT_VERIFIED(UNAUTHORIZED,"USER_008","이메일 인증이 완료되지 않았습니다."),
    INVALID_DEPARTMENT(BAD_REQUEST,"USER_009","해당하는 학과가 존재하지 않습니다."),
    PASSWORD_REQUIRED(BAD_REQUEST,"USER_011","변경할 비밀번호를 입력해주세요."),
    INVALID_PASSWORD_CONFIRM(BAD_REQUEST,"USER_012","비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    DUPLICATE_BOJ_ID(CONFLICT, "USER_013", "이미 등록된 BOJ 아이디입니다."),
    DUPLICATE_STUDENT_ID(CONFLICT, "USER_014", "이미 등록된 학번입니다."),
    DUPLICATE_PHONE_NUMBER(CONFLICT, "USER_015", "이미 등록된 전화번호입니다."),
    INVALID_USER_GRADE(HttpStatus.BAD_REQUEST, "USER_016", "학년 입력값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
