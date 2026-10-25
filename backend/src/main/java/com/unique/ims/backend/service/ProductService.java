package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.ProductDto;
import java.util.List;

public interface ProductService {
    ProductDto createProduct(ProductDto productDto);
    ProductDto updateProduct(Long id, ProductDto productDto);
    ProductDto getProductById(Long id);
    ProductDto getProductBySku(String sku);
    ProductDto getProductByBarcode(String barcode);
    List<ProductDto> getAllProducts();
    void deleteProduct(Long id);
    List<ProductDto> getLowStockProducts();
}