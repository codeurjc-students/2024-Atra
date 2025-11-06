package codeurjc_students.atra.exception;

import lombok.Getter;

@Getter
public class VisibilityException extends RuntimeException{
    public VisibilityException(String msg) {
        super(msg);
    }

}
