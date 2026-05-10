package by.niruin.library.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint;
    private String user;
    private String password;
    private String bucketName;
    private Long maxFileSize;

    public String getEndpoint() {
        return endpoint;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}
