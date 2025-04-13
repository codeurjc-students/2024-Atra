package codeurjc_students.ATRA.exception;

public class HttpException extends Exception{
    private int status;
    public HttpException(int status) {
        this.status = status;
    }

    public HttpException(int status, String msg) {
        super(msg);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
