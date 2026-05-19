package by.niruin.library.exception.handler;

import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.model.error.ErrorResponse;
import feign.FeignException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException exception) {
        log.warn("Exception: {}", exception.getMessage());

        var errorResponse = new ErrorResponse("Entity not found", exception.getMessage(),
                HttpStatus.NOT_FOUND.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        var errorResponse = new ErrorResponse("Validation error", validationErrors, HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleEntityAlready(EntityAlreadyExistException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Entity already exist", exception.getMessage(),
                HttpStatus.CONFLICT.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException exception) {
        int status = exception.status() >= 400 ? exception.status() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        ErrorResponse errorResponse;
        if (exception.responseBody().isPresent()) {
            try {
                errorResponse = parseFeignException(exception);

                return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(status));
            } catch (Exception e) {
                log.warn("Parsing JSON error from feign exception", e);
            }
        }

        var fallbackResponse = new ErrorResponse("Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(fallbackResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Entity already exist in database", exception.getMessage(),
                HttpStatus.CONFLICT.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("Unknown exception occurred: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse parseFeignException(FeignException exception) {
        byte[] rawBody = exception.responseBody().get().array();

        return objectMapper.readValue(rawBody, ErrorResponse.class);
    }
}
