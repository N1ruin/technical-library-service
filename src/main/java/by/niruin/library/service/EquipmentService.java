package by.niruin.library.service;

import by.niruin.library.domain.Equipment;
import by.niruin.library.domain.EquipmentType;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.model.equipment.UpdateEquipmentRequest;
import by.niruin.library.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class EquipmentService {
    private final FileService fileService;
    private final EquipmentRepository equipmentRepository;

    public EquipmentService(FileService fileService, EquipmentRepository equipmentRepository) {
        this.fileService = fileService;
        this.equipmentRepository = equipmentRepository;
    }

    public Equipment save(Equipment equipment, MultipartFile image) {
        if (equipmentRepository.existsByIndex(equipment.getIndex())) {
            throw new EntityAlreadyExistException();
        }

        String imageName = null;
        if (image != null && !image.isEmpty()) {
            imageName = fileService.uploadImage(image);
        }
        equipment.setImageName(imageName);

        try {
            return equipmentRepository.save(equipment);
        } catch (Exception e) {
            if (imageName != null) {
                fileService.deleteImage(imageName);
            }
            throw e;
        }
    }

    public Equipment findById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
    }

    public List<Equipment> findAll() {
        return equipmentRepository.findAll();
    }

    public Equipment update(Long id, UpdateEquipmentRequest request) {
        var equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        if (!equipment.getIndex().equals(request.index()) && equipmentRepository.existsByIndex(request.index())) {
            throw new EntityAlreadyExistException();
        }

        updateFields(equipment, request);

        return equipment;
    }

    private void updateFields(Equipment equipment, UpdateEquipmentRequest request) {
        equipment.setName(request.name());
        equipment.setIndex(request.index());
        equipment.setDescription(request.description());
        equipment.setType(EquipmentType.valueOf(request.type()));

        if (request.file() != null) {
            var newFileName = replaceImage(equipment.getImageName(), request.file());
            equipment.setImageName(newFileName);
        }
    }

    private String replaceImage(String oldFileName, MultipartFile file) {
        if (oldFileName != null) {
            fileService.deleteImage(oldFileName);
        }

        return fileService.uploadImage(file);
    }

    public void deleteById(Long id) {
        var equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        equipmentRepository.delete(equipment);
        fileService.deleteImage(equipment.getImageName());//todo transactionOutbox везде где работа с файлами (сохранение и удаление)
    }
}
