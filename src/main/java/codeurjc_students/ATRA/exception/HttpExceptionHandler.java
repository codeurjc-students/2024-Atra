package codeurjc_students.ATRA.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class HttpExceptionHandler {

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<Map<String, String>> handleHttpException(HttpException e) {
        Map<String, String> map = new HashMap<>();
        map.put("status", Integer.toString(e.getStatus()));
        map.put("message", e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(map);
    }
}
