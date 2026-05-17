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

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 10)
    public void produce() {
        var records = transactionOutboxService.findBatchRecords(10);
        for (var record : records) {
            try {
                var result = kafkaTemplate.send(
                                record.getEventType().getTopicName(),
                                record.getId().toString(),
                                record.getPayload())
                        .get(5, TimeUnit.SECONDS);

                transactionOutboxService.delete(record);

                logger.info("Message sent successfully! Topic: {}, Partition: {}, offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } catch (TimeoutException e) {
                logger.error("Timeout sending message: {}", record.getId(), e);
            } catch (Exception e) {
                logger.error("Failed to send message: {}", record.getId(), e);
                break;
            }
        }
    }
}
