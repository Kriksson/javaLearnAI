package learning.task062;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO: зарегистрируй этот класс как @RestControllerAdvice.
@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        // TODO: собери отсортированные уникальные имена неверных полей.
        List<String> errorFields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .toList();
        ApiError apiError = new ApiError("invalid_request", (List<String>) errorFields);
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedJson(HttpMessageNotReadableException exception) {
        // TODO: верни код malformed_json и пустой список полей.
        ApiError apiError = new ApiError("malformed_json", List.of());
        return ResponseEntity.badRequest().body(apiError);
    }
}
