package by.niruin.library.service;

import by.niruin.library.kafka.EventPublisher;
import by.niruin.library.domain.Material;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.model.material.UpdateMaterialRequest;
import by.niruin.library.repository.MaterialRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final EventPublisher eventPublisher;

    public MaterialService(MaterialRepository materialRepository, EventPublisher eventPublisher) {
        this.materialRepository = materialRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @CachePut(value = "material", key = "#result.id", unless = "#result == null")
    public Material save(Material material) {
        var name = material.getName();
        var standard = material.getStandard();

        if (materialRepository.existsByNameAndStandard(name, standard)) {
            throw new EntityAlreadyExistException();
        }

        var savedMaterial = materialRepository.save(material);

        eventPublisher.publishMaterialSavedEvent(savedMaterial);

        return savedMaterial;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "material", key = "#id", sync = true)
    public Material findById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Material> findAll(Pageable pageable) {
        return materialRepository.findAll(pageable);
    }

    @Transactional
    @CachePut(value = "material", key = "#result.id")
    public Material update(Long id, UpdateMaterialRequest request) {
        var material = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        if (isNameOrStandardChanged(material, request)
                && materialRepository.existsByNameAndStandard(request.name(), request.standard())) {
            throw new EntityAlreadyExistException();
        }

        updateFields(material, request);

        eventPublisher.publishMaterialUpdatedEvent(material);

        return material;
    }

    @Transactional
    @CacheEvict(value = "material", key = "#id")
    public void deleteById(Long id) {
        var deletedMaterial = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        eventPublisher.publishMaterialDeletedEvent(deletedMaterial);

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
