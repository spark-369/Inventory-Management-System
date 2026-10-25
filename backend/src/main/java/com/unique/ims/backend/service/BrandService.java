package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.BrandDto;
import java.util.List;

public interface BrandService {
    BrandDto createBrand(BrandDto brandDto);
    BrandDto updateBrand(Long id, BrandDto brandDto);
    BrandDto getBrandById(Long id);
    List<BrandDto> getAllBrands();
    void deleteBrand(Long id);
}