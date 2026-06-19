package com.securebank.controller;

import com.securebank.dto.CustomerDtos.CustomerResponse;
import com.securebank.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Customer profile endpoint ({@code GET /customers/me}).
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profile")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Get the authenticated customer's profile")
    @GetMapping("/me")
    public CustomerResponse me(Principal principal) {
        return customerService.getMyProfile(principal.getName());
    }
}
