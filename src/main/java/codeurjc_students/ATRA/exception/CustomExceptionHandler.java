package codeurjc_students.atra.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class CustomExceptionHandler {
    static final Integer ENTITY_NOT_FOUND_EXCEPTION_CODE = 404;
    static final Integer INCORRECT_PARAMETERS_EXCEPTION_CODE = 400;
    static final Integer PERMISSION_EXCEPTION_CODE = 403;
    static final Integer VISIBILITY_EXCEPTION_CODE = 403;
    @ExceptionHandler(HttpException.class)
    public ResponseEntity<Map<String, String>> handleHttpException(HttpException e) {
        return helper(e, e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException e) {
        return helper(e, ENTITY_NOT_FOUND_EXCEPTION_CODE, "Could not find the requested entity. It might have been deleted");
    }

    @ExceptionHandler(IncorrectParametersException.class)
    public ResponseEntity<Map<String, String>> handleIncorrectParametersException(IncorrectParametersException e) {
        return helper(e, INCORRECT_PARAMETERS_EXCEPTION_CODE,
                "We couldn't read your request. Double check you're sending the right data with the correct format.");

    }

    @ExceptionHandler(PermissionException.class)
    public ResponseEntity<Map<String, String>> handlePermissionException(PermissionException e) {
        return helper(e, PERMISSION_EXCEPTION_CODE, "You do not have the necessary permissions to perform that operation.");
    }

    @ExceptionHandler(VisibilityException.class)
    public ResponseEntity<Map<String, String>> handleVisibilityException(VisibilityException e) {
        return helper(e, VISIBILITY_EXCEPTION_CODE, "You have no visibility of the specified entity, or it does not exist.");
    }

    private ResponseEntity<Map<String, String>> helper(Exception e, Integer code, String msg) {
        Map<String, String> map = new HashMap<>();
        map.put("status", code.toString());
        if (e.getMessage()!=null) map.put("message", e.getMessage());
        else map.put("message", msg);
        return ResponseEntity.status(code).body(map);
    }
}
