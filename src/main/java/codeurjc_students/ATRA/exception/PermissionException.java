package codeurjc_students.ATRA.exception;

import lombok.Getter;

@Getter
public class PermissionException extends RuntimeException{
    public PermissionException(String msg) {
        super(msg);
    }

}
