package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.SupplierDto;
import com.unique.ims.backend.model.Supplier;
import com.unique.ims.backend.repository.SupplierRepository;
import com.unique.ims.backend.service.SupplierService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.SUPPLIERS, allEntries = true)
    public SupplierDto createSupplier(SupplierDto dto) {
        Supplier supplier = new Supplier();
        supplier.setName(dto.getName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setTaxId(dto.getTaxId());
        supplier.setActive(dto.isActive());
        return mapToDto(supplierRepository.save(supplier));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.SUPPLIERS, allEntries = true)
    public SupplierDto updateSupplier(Long id, SupplierDto dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        if (dto.getName() != null && !dto.getName().isBlank()) {
            supplier.setName(dto.getName());
        }
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setTaxId(dto.getTaxId());
        supplier.setActive(dto.isActive());
        return mapToDto(supplierRepository.save(supplier));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.SUPPLIERS, key = "#id")
    public SupplierDto getSupplierById(Long id) {
        return mapToDto(supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.SUPPLIERS, key = "'all'")
    public List<SupplierDto> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .sorted(Comparator.comparing(Supplier::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.SUPPLIERS, allEntries = true)
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new IllegalArgumentException("Supplier not found: " + id);
        }
        supplierRepository.deleteById(id);
    }

    private SupplierDto mapToDto(Supplier supplier) {
        SupplierDto dto = new SupplierDto();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setContactPerson(supplier.getContactPerson());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setTaxId(supplier.getTaxId());
        dto.setActive(supplier.isActive());
        return dto;
    }
}
