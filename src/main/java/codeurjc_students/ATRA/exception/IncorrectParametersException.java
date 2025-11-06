package codeurjc_students.atra.exception;

import lombok.Getter;

@Getter
public class IncorrectParametersException extends RuntimeException{
    public IncorrectParametersException(String msg) {
        super(msg);
    }
    public IncorrectParametersException(String msg, Exception cause) {
        super(msg, cause);

    }

}
