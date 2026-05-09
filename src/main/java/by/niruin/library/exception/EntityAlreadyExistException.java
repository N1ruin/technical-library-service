package by.niruin.library.exception;

public class EntityAlreadyExistException extends RuntimeException {
    public EntityAlreadyExistException() {
        super("Entity already exist!");
    }
}
