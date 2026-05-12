package by.niruin.library.service;

import by.niruin.library.domain.EventType;
import by.niruin.library.mapper.MaterialMapper;
import by.niruin.library.domain.Material;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.model.material.UpdateMaterialRequest;
import by.niruin.library.repository.MaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final MaterialMapper materialMapper;
    private final TransactionOutboxService transactionOutboxService;

    public MaterialService(MaterialRepository materialRepository, MaterialMapper materialMapper,
                           TransactionOutboxService transactionOutboxService) {
        this.materialRepository = materialRepository;
        this.materialMapper = materialMapper;
        this.transactionOutboxService = transactionOutboxService;
    }

    public Material save(Material material) {
        var name = material.getName();
        var standard = material.getStandard();
        if (materialRepository.existsByNameAndStandard(name, standard)) {
            throw new EntityAlreadyExistException();
        }

        var savedMaterial = materialRepository.save(material);

        var outboxRecord = transactionOutboxService.createOutboxRecord(EventType.MATERIAL_CREATED,
                savedMaterial,
                materialMapper::toSavedEvent);

        transactionOutboxService.save(outboxRecord);

        return savedMaterial;
    }

    @Transactional(readOnly = true)
    public Material findById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Material> findAll(Pageable pageable) {
        return materialRepository.findAll(pageable);
    }

    public Material update(Long id, UpdateMaterialRequest request) {
        var material = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        if (isNameOrStandardChanged(material, request)
                && materialRepository.existsByNameAndStandard(request.name(), request.standard())) {
            throw new EntityAlreadyExistException();
        }

        updateFields(material, request);

        var outboxRecord = transactionOutboxService.createOutboxRecord(EventType.MATERIAL_UPDATED,
                material,
                materialMapper::toUpdatedEvent);

        transactionOutboxService.save(outboxRecord);

        return material;
    }

    public void deleteById(Long id) {
        var deletedMaterial = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        var outboxRecord = transactionOutboxService.createOutboxRecord(EventType.MATERIAL_DELETED,
                deletedMaterial,
                materialMapper::toDeleteEvent);

        transactionOutboxService.save(outboxRecord);
        materialRepository.deleteById(id);
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
