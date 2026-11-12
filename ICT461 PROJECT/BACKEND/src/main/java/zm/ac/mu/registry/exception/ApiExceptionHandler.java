package zm.ac.mu.registry.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import zm.ac.mu.registry.web.ApiFailure;
import zm.ac.mu.registry.web.Problem;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(ApiExceptionHandler.class.getName());

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = null;
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof UnrecognizedPropertyException unknown) {
            field = unknown.getPropertyName();
        } else if (cause instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            field = mapping.getPath().get(mapping.getPath().size() - 1).getFieldName();
        }
        return response(HttpStatus.BAD_REQUEST, "Request body could not be parsed.",
                field == null ? List.of() : List.of(new Problem.FieldError(field, "Invalid or unexpected value.")), headers);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Problem.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError).toList();
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "Request validation failed.", errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<Problem.FieldError> errors = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error -> errors.add(new Problem.FieldError(
                        result.getMethodParameter().getParameterName() == null
                                ? "request" : result.getMethodParameter().getParameterName(),
                        error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage()))));
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "Request validation failed.", errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String field = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName() : "request";
        return response(HttpStatus.BAD_REQUEST, "A request parameter has the wrong type.",
                List.of(new Problem.FieldError(field, "Invalid value type.")), headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json.",
                List.of(), headers);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        HttpStatus safeStatus = resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
        if (safeStatus.is5xxServerError()) LOGGER.log(Level.SEVERE, "Unhandled API exception", exception);
        return response(safeStatus, safeStatus.is5xxServerError()
                        ? "An unexpected server error occurred." : safeStatus.getReasonPhrase(),
                List.of(), headers);
    }

    @ExceptionHandler(ApiFailure.class)
    public ResponseEntity<Problem> handleApiFailure(ApiFailure exception) {
        List<Problem.FieldError> errors = exception.field() == null ? List.of()
                : List.of(new Problem.FieldError(exception.field(), exception.getMessage()));
        return problem(exception.status(), exception.getMessage(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolation(ConstraintViolationException exception) {
        List<Problem.FieldError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new Problem.FieldError(
                        violation.getPropertyPath().toString().replaceAll("^.*\\.", ""),
                        violation.getMessage())).toList();
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Request validation failed.", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleUnexpected(Exception exception) {
        LOGGER.log(Level.SEVERE, "Unhandled API exception", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.", List.of());
    }

    private Problem.FieldError fieldError(FieldError error) {
        return new Problem.FieldError(error.getField(),
                error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage());
    }

    private ResponseEntity<Object> response(HttpStatus status, String detail,
            List<Problem.FieldError> errors, HttpHeaders headers) {
        Problem body = body(status, detail, errors);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(body, responseHeaders, status);
    }

    private ResponseEntity<Problem> problem(HttpStatus status, String detail, List<Problem.FieldError> errors) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body(status, detail, errors));
    }

    private Problem body(HttpStatus status, String detail, List<Problem.FieldError> errors) {
        return new Problem("about:blank", status.getReasonPhrase(), status.value(), detail, errors);
    }
}
