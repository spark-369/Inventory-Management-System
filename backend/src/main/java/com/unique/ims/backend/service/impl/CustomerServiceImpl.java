package com.unique.ims.backend.service.impl;

import com.unique.ims.backend.config.CacheNames;
import com.unique.ims.backend.dto.CustomerDto;
import com.unique.ims.backend.model.Customer;
import com.unique.ims.backend.repository.CustomerRepository;
import com.unique.ims.backend.service.CustomerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.CUSTOMERS, allEntries = true)
    public CustomerDto createCustomer(CustomerDto dto) {
        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setTaxId(dto.getTaxId());
        customer.setActive(dto.isActive());
        return mapToDto(customerRepository.save(customer));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.CUSTOMERS, allEntries = true)
    public CustomerDto updateCustomer(Long id, CustomerDto dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        if (dto.getName() != null && !dto.getName().isBlank()) {
            customer.setName(dto.getName());
        }
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setTaxId(dto.getTaxId());
        customer.setActive(dto.isActive());
        return mapToDto(customerRepository.save(customer));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CUSTOMERS, key = "#id")
    public CustomerDto getCustomerById(Long id) {
        return mapToDto(customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CUSTOMERS, key = "'all'")
    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .sorted(Comparator.comparing(Customer::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.CUSTOMERS, allEntries = true)
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found: " + id);
        }
        customerRepository.deleteById(id);
    }

    private CustomerDto mapToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setTaxId(customer.getTaxId());
        dto.setActive(customer.isActive());
        return dto;
    }
}
