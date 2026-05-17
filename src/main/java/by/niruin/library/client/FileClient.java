package by.niruin.library.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "file-service", path = "/api/v1/files", url = "${file-service.url:http://file-service}")
public interface FileClient {
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadImage(@RequestPart("file") MultipartFile file);

    @DeleteMapping("/{fileName}")
    void deleteImage(@PathVariable String fileName);
}
