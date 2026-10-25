package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.CategoryDto;
import com.unique.ims.backend.model.Category;
import com.unique.ims.backend.repository.CategoryRepository;
import com.unique.ims.backend.service.CategoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.CATEGORIES, allEntries = true)
    public CategoryDto createCategory(CategoryDto categoryDto) {
        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setImageUrl(categoryDto.getImageUrl());
        return mapToDto(categoryRepository.save(category));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.CATEGORIES, allEntries = true)
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        if (categoryDto.getName() != null && !categoryDto.getName().isBlank()) {
            category.setName(categoryDto.getName());
        }
        category.setDescription(categoryDto.getDescription());
        category.setImageUrl(categoryDto.getImageUrl());
        return mapToDto(categoryRepository.save(category));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "#id")
    public CategoryDto getCategoryById(Long id) {
        return mapToDto(categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "'all'")
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.CATEGORIES, allEntries = true)
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    private CategoryDto mapToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setImageUrl(category.getImageUrl());
        return dto;
    }
}
