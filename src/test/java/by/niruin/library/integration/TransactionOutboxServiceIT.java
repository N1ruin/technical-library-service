package by.niruin.library.integration;

import by.niruin.library.kafka.KafkaProducer;
import by.niruin.library.repository.TransactionOutboxRepository;
import by.niruin.library.service.TransactionOutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

public class TransactionOutboxServiceIT extends BaseIntegrationTest {
    @Autowired
    private TransactionOutboxRepository outboxRepository;
    @Autowired
    private KafkaProducer kafkaProducer;

}
//@Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 5)
//public void produce() {
//    var records = transactionOutboxService.findBatchRecords(10);
//    for (var record : records) {
//        kafkaTemplate.send(
//                        record.getEventType().getTopicName(),
//                        record.getId().toString(),
//                        record.getPayload())
//                .toCompletableFuture()
//                .whenComplete((result, exception) -> {
//                    if (exception == null) {
//                        var metadata = result.getRecordMetadata();
//                        logger.info("Message sent successfully! Topic: {}, Partition: {}, offset: {}",
//                                metadata.topic(),
//                                metadata.partition(),
//                                metadata.offset());
//
//                        transactionOutboxService.delete(record);
//                    } else {
//                        logger.error("Failed to send message: {}", exception.getMessage(), exception);
//                    }
//                });
//    }
}