package com.mealguide.mealguide_api.global.base.exception;

import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
//@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ResponseBody<Void>> handleServiceException(ServiceException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ResponseUtils.createFailureResponse(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseBody<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        String customMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity
                .status(ErrorCode.BINDING_ERROR.getStatus())
                .body(ResponseUtils.createFailureResponse(ErrorCode.BINDING_ERROR, customMessage));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseBody<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException", e);
        return ResponseEntity
                .status(ErrorCode.BINDING_ERROR.getStatus())
                .body(ResponseUtils.createFailureResponse(ErrorCode.BINDING_ERROR));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseBody<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_HTTP_METHOD.getStatus())
                .body(ResponseUtils.createFailureResponse(ErrorCode.INVALID_HTTP_METHOD));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseBody<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_ENDPOINT.getStatus())
                .body(ResponseUtils.createFailureResponse(ErrorCode.INVALID_ENDPOINT));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseBody<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("AccessDeniedException", e);
        return ResponseEntity
                .status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(ResponseUtils.createFailureResponse(ErrorCode.ACCESS_DENIED));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ResponseBody<Void>> handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException e) {
        return ResponseEntity
                .status(ErrorCode.NEED_AUTHORIZED.getStatus())
                .body(ResponseUtils.createFailureResponse(ErrorCode.NEED_AUTHORIZED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseBody<Void>> handleException(Exception e){
        log.error("Unhandle Exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtils.createFailureResponse(ErrorCode.UNEXPECTED_SERVER_ERROR));
    }
}
