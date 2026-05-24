package by.niruin.library.kafka;

import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.mapper.OutboxRecordMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OutboxRecordProducer {
    private static final Logger logger = LogManager.getLogger(OutboxRecordProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxRecordMapper eventDeserializer;

    public OutboxRecordProducer(KafkaTemplate<String, Object> kafkaTemplate, OutboxRecordMapper eventDeserializer) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventDeserializer = eventDeserializer;
    }

    public CompletableFuture<SendResult<String, Object>> sendOutboxRecord(TransactionOutboxRecord record) {
        try {
            Object event = eventDeserializer.mapRecordToJson(record);

            return kafkaTemplate.send(
                            record.getEventType().getTopicName(),
                            record.getId().toString(),
                            event)
                    .toCompletableFuture();
        } catch (Exception e) {
            logger.error("Failed to send event: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
