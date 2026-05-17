package by.niruin.library.service;

import by.niruin.library.model.event.EventType;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.model.event.MessageBrokerEvent;
import by.niruin.library.repository.TransactionOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.function.Function;

@Service
@Transactional
public class TransactionOutboxService {
    private final TransactionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public TransactionOutboxService(TransactionOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void save(TransactionOutboxRecord outboxRecord) {
        outboxRepository.save(outboxRecord);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInNewTransaction(TransactionOutboxRecord outboxRecord) {
        outboxRepository.save(outboxRecord);
    }

    public <T extends MessageBrokerEvent, O> TransactionOutboxRecord createOutboxRecord(EventType eventType, O object,
                                                                                        Function<O, T> eventMapper) {
        var outboxRecord = new TransactionOutboxRecord();
        outboxRecord.setEventType(eventType);

        var event = eventMapper.apply(object);
        outboxRecord.setPayload(objectMapper.writeValueAsString(event));

        return outboxRecord;
    }

    public void delete(TransactionOutboxRecord record) {
        outboxRepository.delete(record);
    }

    public List<TransactionOutboxRecord> findBatchRecords(int batchSize) {
        return outboxRepository.findTopByOrderById(PageRequest.of(0, batchSize));
    }
}
