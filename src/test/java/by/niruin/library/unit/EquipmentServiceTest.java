package by.niruin.library.unit;

import by.niruin.library.mapper.EquipmentMapper;
import by.niruin.library.repository.EquipmentRepository;
import by.niruin.library.service.EquipmentService;
import by.niruin.library.service.TransactionOutboxService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EquipmentServiceTest {
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private TransactionOutboxService outboxService;
    @Mock
    private EquipmentMapper equipmentMapper;
    @InjectMocks
    private EquipmentService equipmentService;

//    @Test
//    void saveSuccess_shouldReturnSavedEquipment() {
//        var instruction = createTestInstruction();
//        when(instructionRepository.existsByNumber(instruction.getNumber()))
//                .thenReturn(false);
//        when(instructionRepository.save(instruction))
//                .thenReturn(instruction);
//        var outboxRecord = mock(TransactionOutboxRecord.class);
//        when(outboxService.createOutboxRecord(eq(EventType.SAFETY_INSTRUCTION_CREATED), eq(instruction), any()))
//                .thenReturn(outboxRecord);
//
//        var saved = instructionService.save(instruction);
//
//        assertThat(instruction).usingRecursiveComparison()
//                .isEqualTo(saved);
//        verify(outboxService).createOutboxRecord(eq(EventType.SAFETY_INSTRUCTION_CREATED), eq(instruction), any());
//        verify(instructionRepository).existsByNumber(instruction.getNumber());
//        verify(instructionRepository).save(instruction);
//        verify(outboxService).save(outboxRecord);
//    }
//
//    private Equipment createTestEquipment() {
//        var equipment = new Equipment();
//        instruction.setNumber("123п");
//        instruction.setDescription("Для слесарей механосборочных работ");
//
//        return equipment;
//    }
//
//    private UpdateEquipmentRequest createUpdateEquipmentRequest() {
//        var number = "322";
//        var description = "Для слесарей МСР";
//
//        return new UpdateEquipmentRequest(number, description);
//    }
}
