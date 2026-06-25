package by.niruin.library.client.fallbackFactory;

import by.niruin.library.client.FileClient;
import by.niruin.library.exception.FileUploadException;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class FileClientFallbackFactory implements FallbackFactory<FileClient> {
    @Override
    public FileClient create(Throwable cause) {
        return file -> {
            if (cause instanceof FeignException feignException) {
                throw new FileUploadException(
                        "File service error: " + feignException.getMessage(),
                        cause,
                        feignException.status());
            }
            throw new FileUploadException("File service is temporarily unavailable", cause);
        };
    }
}
