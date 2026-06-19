package com.securebank.service;

import com.securebank.config.Audited;
import com.securebank.domain.Beneficiary;
import com.securebank.domain.Customer;
import com.securebank.dto.BeneficiaryDtos.BeneficiaryResponse;
import com.securebank.dto.BeneficiaryDtos.CreateBeneficiaryRequest;
import com.securebank.exception.ResourceNotFoundException;
import com.securebank.mapper.BeneficiaryMapper;
import com.securebank.repository.BeneficiaryRepository;
import com.securebank.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Beneficiary (saved payee) management for the authenticated customer.
 * Creating a beneficiary is {@link Audited}.
 */
@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;
    private final BeneficiaryMapper beneficiaryMapper;

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listMine(String username) {
        return beneficiaryMapper.toResponseList(
                beneficiaryRepository.findByCustomerUserUsername(username));
    }

    @Transactional
    @Audited(action = "BENEFICIARY_ADDED", entityType = "BENEFICIARY")
    public BeneficiaryResponse create(String username, CreateBeneficiaryRequest req) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("error.customer.notFound"));
        Beneficiary beneficiary = Beneficiary.builder()
                .customer(customer)
                .name(req.name())
                .accountNumber(req.accountNumber())
                .bankName(req.bankName())
                .build();
        return beneficiaryMapper.toResponse(beneficiaryRepository.save(beneficiary));
    }
}
