package by.niruin.library.unit;

import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.kafka.KafkaProducer;
import by.niruin.library.model.event.EventType;
import by.niruin.library.service.TransactionOutboxService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaProducerTest {
    @Mock
    private TransactionOutboxService outboxService;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @InjectMocks
    private KafkaProducer kafkaProducer;

    @Test
    void should_SendToKafka_And_DeleteFromOutbox_When_Success() {
        var topicName = EventType.EQUIPMENT_CREATED.getTopicName();
        var record = mock(TransactionOutboxRecord.class);
        when(record.getEventType()).thenReturn(EventType.EQUIPMENT_CREATED);
        when(record.getId()).thenReturn(1L);
        when(record.getPayload()).thenReturn("test-payload");
        when(outboxService.findBatchRecords(10)).thenReturn(List.of(record));
        var topicPartition = new TopicPartition(topicName, 0);
        var metadata = new RecordMetadata(topicPartition, 0L, 0, 0L, 0, 0);
        var producerRecord = new ProducerRecord<String, String>(topicName, "test-payload");
        var sendResult = new SendResult<>(producerRecord, metadata);
        var future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        kafkaProducer.produce();

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxService, times(1)).delete(record));
        verify(kafkaTemplate, times(1)).send(topicName, record.getId().toString(), "test-payload");
    }

    @Test
    void should_NotDeleteFromOutbox_When_KafkaThrowsException() {
        var record = new TransactionOutboxRecord();
        record.setEventType(EventType.EQUIPMENT_CREATED);
        record.setPayload("test-payload");
        ReflectionTestUtils.setField(record, "id", 1L);
        when(outboxService.findBatchRecords(10)).thenReturn(List.of(record));
        var future = new CompletableFuture<SendResult<String, String>>();
        future.completeExceptionally(new RuntimeException());
        doReturn(future).when(kafkaTemplate).send(any(), any(), any());

        kafkaProducer.produce();

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(outboxService, never()).delete(any());
                });
    }
}
