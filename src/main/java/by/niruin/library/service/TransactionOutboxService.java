package by.niruin.library.service;

import by.niruin.library.model.event.EventType;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.model.event.MessageBrokerEvent;
import by.niruin.library.repository.TransactionOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Service
public class TransactionOutboxService {
    private final TransactionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public TransactionOutboxService(TransactionOutboxRepository outboxRepository, ObjectMapper objectMapper,
                                    TransactionTemplate transactionTemplate) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public void save(TransactionOutboxRecord outboxRecord) {
        outboxRepository.save(outboxRecord);
    }

    public <T extends MessageBrokerEvent, O> TransactionOutboxRecord createOutboxRecord(EventType eventType, O object,
                                                                                        Function<O, T> eventMapper) {
        var outboxRecord = new TransactionOutboxRecord();
        outboxRecord.setEventType(eventType);

        var event = eventMapper.apply(object);
        outboxRecord.setPayload(objectMapper.writeValueAsString(event));
        outboxRecord.setTimestamp(Instant.now());

        return outboxRecord;
    }

    @Transactional(readOnly = true)
    public List<TransactionOutboxRecord> findBatchRecords(int batchSize) {
        return outboxRepository.findAllByOrderByIdAsc(PageRequest.of(0, batchSize));
    }

    public void deleteAll(List<TransactionOutboxRecord> sentRecords) {
        Objects.requireNonNull(sentRecords);

        if (sentRecords.isEmpty()) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> outboxRepository.deleteAll(sentRecords));
    }
}
