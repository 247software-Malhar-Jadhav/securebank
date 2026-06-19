package com.securebank.service;

import com.securebank.domain.Customer;
import com.securebank.dto.CustomerDtos.CustomerResponse;
import com.securebank.exception.ResourceNotFoundException;
import com.securebank.mapper.CustomerMapper;
import com.securebank.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read service for the authenticated customer's own profile
 * ({@code GET /customers/me}).
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile(String username) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("error.customer.notFound"));
        return customerMapper.toResponse(customer);
    }
}
