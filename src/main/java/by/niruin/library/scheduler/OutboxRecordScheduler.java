package by.niruin.library.scheduler;

import by.niruin.library.configuration.properties.SchedulingOutboxProperties;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.kafka.OutboxRecordProducer;
import by.niruin.library.service.TransactionOutboxService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Profile("!test")
public class OutboxRecordScheduler {
    private static final Logger logger = LogManager.getLogger(OutboxRecordScheduler.class);
    private final OutboxRecordProducer outboxRecordProducer;
    private final TransactionOutboxService outboxService;
    private final SchedulingOutboxProperties schedulingOutboxProperties;

    public OutboxRecordScheduler(OutboxRecordProducer outboxRecordProducer, TransactionOutboxService outboxService,
                                 SchedulingOutboxProperties schedulingOutboxProperties) {
        this.outboxRecordProducer = outboxRecordProducer;
        this.outboxService = outboxService;
        this.schedulingOutboxProperties = schedulingOutboxProperties;
    }

    @Scheduled(fixedDelayString = "${scheduler.outbox.millis-delay}")
    @Transactional
    public void sendOutboxEvents() {
        var records = outboxService.findBatchRecords(schedulingOutboxProperties.getBatchSize());

        if (records.isEmpty()) {
            return;
        }

        var sentRecords = new ArrayList<TransactionOutboxRecord>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (var record : records) {
            var future = outboxRecordProducer.sendOutboxRecord(record)
                    .thenRun(() -> {
                        synchronized (sentRecords) {
                            sentRecords.add(record);
                        }
                        logger.info("Outbox message {} with id {} sent successfully",
                                record.getEventType().name(), record.getId());
                    })
                    .exceptionally(ex -> {
                        logger.warn("Outbox message {} with id {} sent failure: {}",
                                record.getEventType().name(), record.getId(), ex.getMessage());
                        return null;
                    });

            futures.add(future);
        }

        futures.forEach(CompletableFuture::join);
        outboxService.deleteAll(sentRecords);
    }
}
