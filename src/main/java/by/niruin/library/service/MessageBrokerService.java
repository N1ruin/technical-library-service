package by.niruin.library.service;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class MessageBrokerService {
    private static final Logger logger = LogManager.getLogger(MessageBrokerService.class);
    private final TransactionOutboxService transactionOutboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageBrokerService(TransactionOutboxService transactionOutboxService,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.transactionOutboxService = transactionOutboxService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 10)
    public void sendMessages() {
        for (var record : transactionOutboxService.findBatchRecords(10)) {
            kafkaTemplate.send(
                            record.getEventType().getTopicName(),
                            record.getId().toString(),
                            record.getPayload())
                    .whenComplete((result, e) -> {
                        if (e == null) {
                            transactionOutboxService.delete(record);
                            logger.info("Message sent successfully and deleted: {}", record.getId());
                        } else {
                            logger.error("Failed to send message: {}", record.getId(), e);
                        }
                    });
        }
    }
}
