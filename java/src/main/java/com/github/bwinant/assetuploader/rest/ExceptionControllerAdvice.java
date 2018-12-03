package com.github.bwinant.assetuploader.rest;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.bwinant.assetuploader.AssetNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.*;

/**
 * Handles all exceptions throws from @RestController annotated classes and converts them to appropriate HTTP responses
 */
@ControllerAdvice(annotations = {RestController.class})
public class ExceptionControllerAdvice
{
    private static final Logger log = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, Exception ex)
    {
        log.error("{} {} - Unexpected error", request.getMethod(), request.getRequestURI(), ex);
        return error(INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> invalidRequest(HttpServletRequest request, InvalidRequestException ex)
    {
        log.error("{} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<ErrorResponse> assetNotFound(HttpServletRequest request, AssetNotFoundException ex)
    {
        log.error("{} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(NOT_FOUND, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message)
    {
        return new ResponseEntity<>(new ErrorResponse(message), status);
    }

    private static class ErrorResponse
    {
        private final String message;

        public ErrorResponse(String message)
        {
            this.message = message;
        }

        @JsonProperty("error")
        public String getMessage()
        {
            return message;
        }
    }
}

