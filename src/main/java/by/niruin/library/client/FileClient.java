package by.niruin.library.client;

import by.niruin.library.client.fallbackFactory.FileClientFallbackFactory;
import by.niruin.library.configuration.FeignClientConfiguration;
import by.niruin.library.model.file.UploadFileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "file-service",
        path = "/api/v1/file-service/files/images",
        url = "${file-service.url}",
        fallbackFactory = FileClientFallbackFactory.class,
        configuration = FeignClientConfiguration.class)
public interface FileClient {
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    UploadFileResponse uploadImage(@RequestPart("file") MultipartFile file);
}
