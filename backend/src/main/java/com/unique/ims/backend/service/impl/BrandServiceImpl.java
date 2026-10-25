package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.BrandDto;
import com.unique.ims.backend.model.Brand;
import com.unique.ims.backend.repository.BrandRepository;
import com.unique.ims.backend.service.BrandService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    public BrandServiceImpl(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.BRANDS, allEntries = true)
    public BrandDto createBrand(BrandDto brandDto) {
        Brand brand = new Brand();
        brand.setName(brandDto.getName());
        brand.setDescription(brandDto.getDescription());
        brand.setLogoUrl(brandDto.getLogoUrl());
        return mapToDto(brandRepository.save(brand));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.BRANDS, allEntries = true)
    public BrandDto updateBrand(Long id, BrandDto brandDto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + id));
        if (brandDto.getName() != null && !brandDto.getName().isBlank()) {
            brand.setName(brandDto.getName());
        }
        brand.setDescription(brandDto.getDescription());
        brand.setLogoUrl(brandDto.getLogoUrl());
        return mapToDto(brandRepository.save(brand));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.BRANDS, key = "#id")
    public BrandDto getBrandById(Long id) {
        return mapToDto(brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + id)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.BRANDS, key = "'all'")
    public List<BrandDto> getAllBrands() {
        return brandRepository.findAll().stream()
                .sorted(Comparator.comparing(Brand::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.BRANDS, allEntries = true)
    public void deleteBrand(Long id) {
        brandRepository.deleteById(id);
    }

    private BrandDto mapToDto(Brand brand) {
        BrandDto dto = new BrandDto();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        dto.setDescription(brand.getDescription());
        dto.setLogoUrl(brand.getLogoUrl());
        return dto;
    }
}
