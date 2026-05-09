package by.niruin.library.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(Long id) {
        super("Entity with id %d not found".formatted(id));
    }
}
