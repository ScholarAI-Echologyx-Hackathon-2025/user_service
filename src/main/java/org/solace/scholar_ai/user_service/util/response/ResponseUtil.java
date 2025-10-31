package org.solace.scholar_ai.user_service.util.response;

import java.time.LocalDateTime;
import org.solace.scholar_ai.user_service.dto.response.APIErrorResponse;
import org.solace.scholar_ai.user_service.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for creating standardized API responses.
 */
public final class ResponseUtil {

    private ResponseUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates an APIErrorResponse with the given parameters.
     */
    public static APIErrorResponse createErrorResponse(HttpStatus status, ErrorCode errorCode, String message) {
        return APIErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .code(errorCode.name())
                .message(message)
                .suggestion(errorCode.getSuggestion())
                .build();
    }

    /**
     * Creates a success response with data and HTTP 200 status.
     */
    public static <T> ResponseEntity<ResponseWrapper<T>> success(T data) {
        return ResponseEntity.ok(ResponseWrapper.success(data));
    }

    /**
     * Creates a success response with data and custom HTTP status.
     */
    public static <T> ResponseEntity<ResponseWrapper<T>> success(T data, HttpStatus status) {
        return new ResponseEntity<>(ResponseWrapper.success(data), status);
    }

    /**
     * Creates an error response with the specified status and error details.
     */
    public static <T> ResponseEntity<ResponseWrapper<T>> error(HttpStatus status, ErrorCode errorCode, String message) {
        APIErrorResponse errorResponse = createErrorResponse(status, errorCode, message);
        return ResponseEntity.status(status).body(ResponseWrapper.error(errorResponse));
    }
}
