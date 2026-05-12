package by.niruin.library.service;

import by.niruin.library.domain.EventType;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.model.event.KafkaEvent;
import by.niruin.library.repository.TransactionOutboxRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
@Transactional
public class TransactionOutboxService {
    private final TransactionOutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public TransactionOutboxService(TransactionOutboxRepository outboxRepository, JsonSerializer jsonSerializer) {
        this.outboxRepository = outboxRepository;
        this.jsonSerializer = jsonSerializer;
    }

    public void save(TransactionOutboxRecord outboxRecord) {
        outboxRepository.save(outboxRecord);
    }

    public <T extends KafkaEvent, O> TransactionOutboxRecord createOutboxRecord(EventType eventType, O object,
                                                                                Function<O, T> eventMapper) {
        var outboxRecord = new TransactionOutboxRecord();
        outboxRecord.setEventType(eventType);

        var event = eventMapper.apply(object);
        outboxRecord.setPayload(jsonSerializer.serialize(event));

        return outboxRecord;
    }

    public void delete(TransactionOutboxRecord record) {
        outboxRepository.delete(record);
    }

    public List<TransactionOutboxRecord> findBatchRecords(int batchSize) {
        return outboxRepository.findTopByOrderById(PageRequest.of(0, batchSize));
    }
}
