package by.niruin.library.service;

import by.niruin.library.configuration.MinioProperties;
import by.niruin.library.exception.DeleteFileException;
import by.niruin.library.exception.FileToLargeException;
import by.niruin.library.exception.InvalidImageFormatException;
import by.niruin.library.exception.UploadFileException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png");
    private final MinioClient minioClient;
    private final MinioProperties properties;

    public FileService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public String uploadImage(MultipartFile file) {
        checkFileSize(file);

        var originalFileName = file.getOriginalFilename();
        var extension = extractFileExtension(originalFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidImageFormatException("Invalid image %s format".formatted(originalFileName));
        }

        var newFileName = UUID.randomUUID() + extension;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(newFileName)
                            .stream(file.getInputStream(), file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new UploadFileException("File %s upload error".formatted(originalFileName));
        }

        return newFileName;
    }

    public void deleteImage(String imageName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(imageName)
                            .build()
            );
        } catch (Exception e) {
            throw new DeleteFileException("Failed to delete file %s from MinIO".formatted(imageName));
        }
    }

    private void checkFileSize(MultipartFile file) {
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new FileToLargeException("The file size cannot exceed 2MB");
        }
    }

    private String extractFileExtension(String fileName) {
        return fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase()
                : "";
    }
}
