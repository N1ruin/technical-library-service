package by.niruin.library.exception;

public class FileUploadException extends RuntimeException {
    private final int httpStatus;

    public FileUploadException(String message, Throwable cause) {
        this(message, cause, 500);
    }

    public FileUploadException(String message, Throwable cause, int httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
