package com.surecostproject.takehome.service;

import com.surecostproject.takehome.entity.Drug;
import com.surecostproject.takehome.exception.BulkProcessingException;
import com.surecostproject.takehome.exception.DrugNotFoundException;
import com.surecostproject.takehome.exception.InvalidDrugRequestException;
import com.surecostproject.takehome.repository.DrugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DrugService {
    private final DrugRepository drugRepository;

    @Transactional(readOnly = true)
    public List<Drug> getAllDrugs() {
        return drugRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Drug getDrugById(UUID id) {
        return drugRepository.findById(id)
            .orElseThrow(() -> new DrugNotFoundException(id));
    }

    @Transactional
    public Drug createDrug(Drug drug) {
        validateDrug(drug);
        return drugRepository.save(drug);
    }

    @Transactional
    public List<Drug> createBulkDrugs(List<Drug> drugs) {
        validateBulkDrugs(drugs);
        return drugRepository.saveAll(drugs);
    }

    @Transactional
    public Drug updateDrug(UUID id, Drug drug) {
        validateDrug(drug);
        Drug existingDrug = getDrugById(id);
        
        existingDrug.setName(drug.getName());
        existingDrug.setManufacturerName(drug.getManufacturerName());
        existingDrug.setQuantity(drug.getQuantity());
        existingDrug.setPrice(drug.getPrice());
        
        return drugRepository.save(existingDrug);
    }

    @Transactional
    public List<Drug> updateBulkDrugs(List<Drug> drugs) {
        validateBulkDrugsForUpdate(drugs);
        return drugRepository.saveAll(drugs);
    }

    @Transactional
    public void deleteDrug(UUID id) {
        if (!drugRepository.existsById(id)) {
            throw new DrugNotFoundException(id);
        }
        drugRepository.deleteById(id);
    }

    @Transactional
    public void deleteBulkDrugs(List<UUID> ids) {
        validateBulkDrugsForDeletion(ids);
        drugRepository.deleteAllById(ids);
    }

    // Search methods with pagination
    @Transactional(readOnly = true)
    public Page<Drug> searchByName(String name, Pageable pageable) {
        return drugRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Drug> searchByManufacturer(String manufacturerName, Pageable pageable) {
        return drugRepository.findByManufacturerNameContainingIgnoreCase(manufacturerName, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Drug> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return drugRepository.findByPriceBetween(minPrice, maxPrice, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Drug> getAllDrugs(Pageable pageable) {
        return drugRepository.findAll(pageable);
    }

    private void validateDrug(Drug drug) {
        if (drug.getName() == null || drug.getName().trim().isEmpty()) {
            throw new InvalidDrugRequestException("Drug name cannot be empty");
        }
        if (drug.getManufacturerName() == null || drug.getManufacturerName().trim().isEmpty()) {
            throw new InvalidDrugRequestException("Manufacturer name cannot be empty");
        }
        if (drug.getQuantity() < 0) {
            throw new InvalidDrugRequestException("Quantity cannot be negative");
        }
        if (drug.getPrice() == null || drug.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDrugRequestException("Price cannot be negative");
        }
    }

    private void validateBulkDrugs(List<Drug> drugs) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < drugs.size(); i++) {
            try {
                validateDrug(drugs.get(i));
            } catch (InvalidDrugRequestException e) {
                errors.add(String.format("Drug at index %d: %s", i, e.getMessage()));
            }
        }
        if (!errors.isEmpty()) {
            throw new BulkProcessingException("Bulk validation failed", errors);
        }
    }

    private void validateBulkDrugsForUpdate(List<Drug> drugs) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < drugs.size(); i++) {
            Drug drug = drugs.get(i);
            try {
                validateDrug(drug);
                if (drug.getUid() == null) {
                    errors.add(String.format("Drug at index %d: ID is required for bulk update", i));
                } else if (!drugRepository.existsById(drug.getUid())) {
                    errors.add(String.format("Drug at index %d: Drug not found with id: %s", i, drug.getUid()));
                }
            } catch (InvalidDrugRequestException e) {
                errors.add(String.format("Drug at index %d: %s", i, e.getMessage()));
            }
        }
        if (!errors.isEmpty()) {
            throw new BulkProcessingException("Bulk update validation failed", errors);
        }
    }

    private void validateBulkDrugsForDeletion(List<UUID> ids) {
        List<String> errors = new ArrayList<>();
        for (UUID id : ids) {
            if (!drugRepository.existsById(id)) {
                errors.add("Drug not found with id: " + id);
            }
        }
        if (!errors.isEmpty()) {
            throw new BulkProcessingException("Bulk deletion validation failed", errors);
        }
    }
} 