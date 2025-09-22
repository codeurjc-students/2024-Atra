package codeurjc_students.ATRA.exception;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException{

    //possibly substitute for NoSuchElementException, to use existing ones
    public EntityNotFoundException(String msg) {
        super(msg);
    }

}
