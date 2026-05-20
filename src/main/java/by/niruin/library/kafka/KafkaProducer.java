package by.niruin.library.kafka;

import by.niruin.library.service.TransactionOutboxService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaProducer {
    private static final Logger logger = LogManager.getLogger(KafkaProducer.class);
    private final TransactionOutboxService transactionOutboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducer(TransactionOutboxService transactionOutboxService,
                         KafkaTemplate<String, String> kafkaTemplate) {
        this.transactionOutboxService = transactionOutboxService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 5)
    public void produce() {
        var records = transactionOutboxService.findBatchRecords(10);
        for (var record : records) {
            kafkaTemplate.send(
                            record.getEventType().getTopicName(),
                            record.getId().toString(),
                            record.getPayload())
                    .toCompletableFuture()
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            var metadata = result.getRecordMetadata();
                            logger.info("Message sent successfully. Topic: {}, Partition: {}, offset: {}",
                                    metadata.topic(),
                                    metadata.partition(),
                                    metadata.offset());

                            transactionOutboxService.delete(record);
                        } else {
                            logger.error("Failed to send message: {}", exception.getMessage(), exception);
                        }
                    });
        }
    }
}
