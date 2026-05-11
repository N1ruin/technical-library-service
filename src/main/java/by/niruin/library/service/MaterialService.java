package by.niruin.library.service;

import by.niruin.library.domain.EventType;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.mapper.MaterialMapper;
import by.niruin.library.domain.Material;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.model.material.UpdateMaterialRequest;
import by.niruin.library.repository.MaterialRepository;
import by.niruin.library.repository.TransactionOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MaterialService {
    private static final String MATERIAL_TOPIC = "materialEventsTopic";
    private final MaterialRepository materialRepository;
    private final TransactionOutboxRepository outboxRepository;
    //    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MaterialMapper materialMapper;
    private final ObjectMapper objectMapper;

    public MaterialService(MaterialRepository materialRepository, TransactionOutboxRepository outboxRepository,//убрал кафку
                           MaterialMapper materialMapper, ObjectMapper objectMapper) {
        this.materialRepository = materialRepository;
        this.outboxRepository = outboxRepository;
//        this.kafkaTemplate = kafkaTemplate;
        this.materialMapper = materialMapper;
        this.objectMapper = objectMapper;
    }

    public Material save(Material material) {
        var name = material.getName();
        var standard = material.getStandard();

        if (materialRepository.existsByNameAndStandard(name, standard)) {
            throw new EntityAlreadyExistException();
        }

        return materialRepository.save(material);
    }

    @Transactional(readOnly = true)
    public Material findById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Material> findAll() {//todo реализовать по страницам
        return materialRepository.findAll();
    }//todo сделать пагинацию

    public void deleteById(Long id) {
        var material = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        var createdEvent = materialMapper.toCreatedEvent(material);
        var outboxRecord = new TransactionOutboxRecord();
        outboxRecord.setEventType(EventType.MATERIAL_CREATED);

        try {
            outboxRecord.setPayload(objectMapper.writeValueAsString(createdEvent));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        outboxRepository.save(outboxRecord);
        materialRepository.delete(material);
    }

    //    @Cacheable(value = "materials", key = "#id")
    public Material update(Long id, UpdateMaterialRequest request) {
        var material = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        if (isNameOrStandardChanged(material, request)
                && materialRepository.existsByNameAndStandard(request.name(), request.standard())) {
            throw new EntityAlreadyExistException();
        }

        updateFields(material, request);
        //todo transaction outbox
//        var updatedEvent = materialMapper.toUpdatedEvent(material);
//        kafkaTemplate.send(MATERIAL_TOPIC, event);

        return material;
    }

    private boolean isNameOrStandardChanged(Material material, UpdateMaterialRequest request) {
        boolean isNameChanged = !material.getName().equals(request.name());
        boolean isStandardChanged = !material.getStandard().equals(request.standard());

        return isNameChanged || isStandardChanged;
    }

    private void updateFields(Material oldMaterial, UpdateMaterialRequest request) {
        oldMaterial.setName(request.name());

        if (request.description() != null) {
            oldMaterial.setDescription(request.description());
        }

        oldMaterial.setStandard(request.standard());
        oldMaterial.setSupplierCode(request.supplierCode());
    }
}
