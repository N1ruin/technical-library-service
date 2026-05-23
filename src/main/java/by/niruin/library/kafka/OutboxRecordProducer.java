package by.niruin.library.kafka;

import by.niruin.library.domain.TransactionOutboxRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OutboxRecordProducer {
    private static final Logger logger = LogManager.getLogger(OutboxRecordProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRecordProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, String>> sendOutboxRecord(TransactionOutboxRecord record) {
        return kafkaTemplate.send(
                        record.getEventType().getTopicName(),
                        record.getId().toString(),
                        record.getPayload())
                .toCompletableFuture();
    }
}
