package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.ProductDto;
import com.unique.ims.backend.model.Product;
import com.unique.ims.backend.repository.BrandRepository;
import com.unique.ims.backend.repository.CategoryRepository;
import com.unique.ims.backend.repository.ProductRepository;
import com.unique.ims.backend.repository.SupplierRepository;
import com.unique.ims.backend.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SupplierRepository supplierRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              BrandRepository brandRepository,
                              SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public ProductDto createProduct(ProductDto dto) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("A product with SKU '" + dto.getSku() + "' already exists.");
        }
        Product product = new Product();
        product.setSku(dto.getSku());
        applyFields(product, dto);
        return mapToDto(productRepository.save(product));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        if (dto.getSku() != null && !dto.getSku().isBlank() && !dto.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(dto.getSku())) {
                throw new IllegalArgumentException("A product with SKU '" + dto.getSku() + "' already exists.");
            }
            product.setSku(dto.getSku());
        }

        applyFields(product, dto);
        return mapToDto(productRepository.save(product));
    }

    private void applyFields(Product product, ProductDto dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setBarcode(dto.getBarcode());
        product.setQrCode(dto.getQrCode());
        product.setPurchasePrice(dto.getPurchasePrice());
        product.setSellingPrice(dto.getSellingPrice());
        product.setMrp(dto.getMrp());
        product.setReorderLevel(dto.getReorderLevel());
        product.setUnit(dto.getUnit());
        product.setActive(dto.isActive());
        product.setTrackBatch(dto.isTrackBatch());
        product.setTrackExpiry(dto.isTrackExpiry());
        if (dto.getCurrentStock() != null) {
            product.setCurrentStock(dto.getCurrentStock());
        }

        product.setCategory(dto.getCategoryId() == null ? null
                : categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.getCategoryId())));
        product.setBrand(dto.getBrandId() == null ? null
                : brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + dto.getBrandId())));
        product.setSupplier(dto.getSupplierId() == null ? null
                : supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + dto.getSupplierId())));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCTS, key = "#id")
    public ProductDto getProductById(Long id) {
        return mapToDto(productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCTS, key = "'sku:' + #sku")
    public ProductDto getProductBySku(String sku) {
        return mapToDto(productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for SKU: " + sku)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCTS, key = "'barcode:' + #barcode")
    public ProductDto getProductByBarcode(String barcode) {
        return mapToDto(productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for barcode: " + barcode)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCTS, key = "'all'")
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, allEntries = true)
    })
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.LOW_STOCK_PRODUCTS, key = "'all'")
    public List<ProductDto> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getCurrentStock() != null && p.getReorderLevel() != null
                        && p.getCurrentStock() <= p.getReorderLevel())
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ProductDto mapToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBarcode(product.getBarcode());
        dto.setQrCode(product.getQrCode());
        dto.setImageUrl(product.getImageUrl());
        dto.setPurchasePrice(product.getPurchasePrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setMrp(product.getMrp());
        dto.setReorderLevel(product.getReorderLevel());
        dto.setCurrentStock(product.getCurrentStock());
        dto.setUnit(product.getUnit());
        dto.setActive(product.isActive());
        dto.setTrackBatch(product.isTrackBatch());
        dto.setTrackExpiry(product.isTrackExpiry());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        if (product.getBrand() != null) {
            dto.setBrandId(product.getBrand().getId());
            dto.setBrandName(product.getBrand().getName());
        }
        if (product.getSupplier() != null) {
            dto.setSupplierId(product.getSupplier().getId());
            dto.setSupplierName(product.getSupplier().getName());
        }
        return dto;
    }
}
