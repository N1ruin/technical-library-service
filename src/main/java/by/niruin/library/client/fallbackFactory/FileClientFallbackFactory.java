package by.niruin.library.client.fallbackFactory;

import by.niruin.library.client.FileClient;
import by.niruin.library.exception.FileUploadException;
import org.springframework.cloud.openfeign.FallbackFactory;

public class FileClientFallbackFactory implements FallbackFactory<FileClient> {
    @Override
    public FileClient create(Throwable cause) {
        return file -> {
            throw new FileUploadException("File service is temporarily unavailable", cause);
        };
    }
}
