package by.niruin.library.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scheduler.outbox")
public class SchedulingOutboxProperties {
    private Integer batchSize = 10;
    private Integer millisDelay = 5000;

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getMillisDelay() {
        return millisDelay;
    }

    public void setMillisDelay(Integer millisDelay) {
        this.millisDelay = millisDelay;
    }
}
