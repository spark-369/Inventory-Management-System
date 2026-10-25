package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.WarehouseDto;
import com.unique.ims.backend.model.Warehouse;
import com.unique.ims.backend.repository.WarehouseRepository;
import com.unique.ims.backend.service.WarehouseService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.WAREHOUSES, allEntries = true)
    public WarehouseDto createWarehouse(WarehouseDto dto) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setCode(dto.getCode());
        warehouse.setAddress(dto.getAddress());
        warehouse.setContactPerson(dto.getContactPerson());
        warehouse.setPhone(dto.getPhone());
        warehouse.setActive(dto.isActive());
        return mapToDto(warehouseRepository.save(warehouse));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.WAREHOUSES, allEntries = true)
    public WarehouseDto updateWarehouse(Long id, WarehouseDto dto) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id));
        if (dto.getName() != null && !dto.getName().isBlank()) {
            warehouse.setName(dto.getName());
        }
        warehouse.setCode(dto.getCode());
        warehouse.setAddress(dto.getAddress());
        warehouse.setContactPerson(dto.getContactPerson());
        warehouse.setPhone(dto.getPhone());
        warehouse.setActive(dto.isActive());
        return mapToDto(warehouseRepository.save(warehouse));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.WAREHOUSES, key = "#id")
    public WarehouseDto getWarehouseById(Long id) {
        return mapToDto(warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.WAREHOUSES, key = "'all'")
    public List<WarehouseDto> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .sorted(Comparator.comparing(Warehouse::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.WAREHOUSES, allEntries = true)
    public void deleteWarehouse(Long id) {
        if (!warehouseRepository.existsById(id)) {
            throw new IllegalArgumentException("Warehouse not found: " + id);
        }
        warehouseRepository.deleteById(id);
    }

    private WarehouseDto mapToDto(Warehouse warehouse) {
        WarehouseDto dto = new WarehouseDto();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setCode(warehouse.getCode());
        dto.setAddress(warehouse.getAddress());
        dto.setContactPerson(warehouse.getContactPerson());
        dto.setPhone(warehouse.getPhone());
        dto.setActive(warehouse.isActive());
        return dto;
    }
}
