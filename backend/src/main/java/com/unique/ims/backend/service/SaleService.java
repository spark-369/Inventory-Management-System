package com.unique.ims.backend.service;

import com.unique.ims.backend.dto.SaleDto;
import java.util.List;

public interface SaleService {
    SaleDto createSale(SaleDto saleDto);
    SaleDto updateSale(Long id, SaleDto saleDto);
    SaleDto getSaleById(Long id);
    SaleDto getSaleByInvoiceNumber(String invoiceNumber);
    List<SaleDto> getAllSales();
    void deleteSale(Long id);
    SaleDto completeSale(Long id);
}