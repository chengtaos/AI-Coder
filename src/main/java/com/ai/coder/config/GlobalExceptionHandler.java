package com.ai.coder.config;

import com.ai.coder.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.concurrent.TimeoutException;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理超时异常
     */
    @ExceptionHandler({TimeoutException.class, AsyncRequestTimeoutException.class})
    public ResponseEntity<ErrorResponseDTO> handleTimeoutException(Exception e, WebRequest request) {
        logger.error("Request timeout occurred", e);

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                "TIMEOUT_ERROR",
                "Request timed out. The operation took too long to complete.",
                "Please try again with a simpler request or check your network connection."
        );

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponseDTO);
    }

    /**
     * 处理AI相关异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntimeException(RuntimeException e, WebRequest request) {
        logger.error("Runtime exception occurred", e);

        // 检查是否是AI调用相关的异常
        String message = e.getMessage();
        if (message != null && (message.contains("tool") || message.contains("function") || message.contains("AI"))) {
            ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                    "AI_TOOL_ERROR",
                    "An error occurred during AI tool execution: " + message,
                    "The AI encountered an issue while processing your request. Please try rephrasing your request or try again."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDTO);
        }

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                "RUNTIME_ERROR",
                "An unexpected error occurred: " + message,
                "Please try again. If the problem persists, contact support."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDTO);
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception e, WebRequest request) {
        logger.error("Unexpected exception occurred", e);

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                "INTERNAL_ERROR",
                "An internal server error occurred",
                "Something went wrong on our end. Please try again later."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDTO);
    }


}
