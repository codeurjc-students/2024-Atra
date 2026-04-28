package codeurjc_students.atra.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{
    public CustomException(String msg) {
        super(msg);
    }
    public CustomException(Exception e) {
        super(e);
    }

    public CustomException(String msg, Exception e) {
        super(msg,e);
    }

}
