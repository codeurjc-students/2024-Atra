package codeurjc_students.atra.exception;

import lombok.Getter;

@Getter
public class HttpException extends RuntimeException{
    private final int status;
    public HttpException(int status) {
        this.status = status;
    }

    public HttpException(int status, String msg) {
        super(msg);
        this.status = status;
    }

}
