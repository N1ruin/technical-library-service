package by.niruin.library.service;

import by.niruin.library.repository.TransactionOutboxRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TransactionOutboxService {
    private static final Logger logger = LogManager.getLogger(TransactionOutboxService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionOutboxRepository outboxRepository;

    public TransactionOutboxService(KafkaTemplate<String, String> kafkaTemplate,
                                    TransactionOutboxRepository outboxRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
    }

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 5)
    @Transactional
    public void sendMessages() {
        var records = outboxRepository.findAll();
        for (var record : records) {
            kafkaTemplate.send(
                    record.getEventType().getTopicName(),
                    record.getId().toString(),
                    record.getPayload()
            ).whenComplete((result, e) -> {
                if (e == null) {
                    outboxRepository.delete(record);
                    logger.info("Message sent successfully: {}", record.getId());
                } else {
                    logger.error("Failed to send message: {}", record.getId(), e);
                }
            });
        }
    }
}
