package codeurjc_students.ATRA.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class CustomExceptionHandler {
    final Integer entityNotFoundExceptionCode = 404;
    final Integer incorrectParametersExceptionCode = 400;
    final Integer permissionExceptionCode = 403;
    final Integer visibilityExceptionCode = 403;
    @ExceptionHandler(HttpException.class)
    public ResponseEntity<Map<String, String>> handleHttpException(HttpException e) {
        return helper(e, e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException e) {
        return helper(e, entityNotFoundExceptionCode, "Could not find the requested entity. It might have been deleted");
    }

    @ExceptionHandler(IncorrectParametersException.class)
    public ResponseEntity<Map<String, String>> handleIncorrectParametersException(IncorrectParametersException e) {
        return helper(e, incorrectParametersExceptionCode,
                "We couldn't read your request. Double check you're sending the right data with the correct format.");

    }

    @ExceptionHandler(PermissionException.class)
    public ResponseEntity<Map<String, String>> handlePermissionException(PermissionException e) {
        return helper(e, permissionExceptionCode, "You do not have the necessary permissions to perform that operation.");
    }

    @ExceptionHandler(VisibilityException.class)
    public ResponseEntity<Map<String, String>> handleVisibilityException(VisibilityException e) {
        return helper(e, visibilityExceptionCode, "You have no visibility of the specified entity, or it does not exist.");
    }

    private ResponseEntity<Map<String, String>> helper(Exception e, Integer code, String msg) {
        Map<String, String> map = new HashMap<>();
        map.put("status", code.toString());
        if (e.getMessage()!=null) map.put("message", e.getMessage());
        else map.put("message", msg);
        return ResponseEntity.status(code).body(map);
    }
}
