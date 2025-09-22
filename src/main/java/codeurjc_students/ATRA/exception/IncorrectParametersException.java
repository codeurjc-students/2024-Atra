package codeurjc_students.ATRA.exception;

import lombok.Getter;

@Getter
public class IncorrectParametersException extends RuntimeException{
    public IncorrectParametersException(String msg) {
        super(msg);
    }

}
