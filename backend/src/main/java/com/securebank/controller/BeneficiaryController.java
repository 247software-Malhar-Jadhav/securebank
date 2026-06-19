package com.securebank.controller;

import com.securebank.dto.BeneficiaryDtos.BeneficiaryResponse;
import com.securebank.dto.BeneficiaryDtos.CreateBeneficiaryRequest;
import com.securebank.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Beneficiary (saved payee) endpoints.
 */
@RestController
@RequestMapping("/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries", description = "Saved payees")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @Operation(summary = "List the authenticated customer's beneficiaries")
    @GetMapping
    public List<BeneficiaryResponse> list(Principal principal) {
        return beneficiaryService.listMine(principal.getName());
    }

    @Operation(summary = "Add a beneficiary")
    @PostMapping
    public ResponseEntity<BeneficiaryResponse> create(Principal principal,
                                                      @Valid @RequestBody CreateBeneficiaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beneficiaryService.create(principal.getName(), request));
    }
}
