package com.securebank.mapper;

import com.securebank.domain.Customer;
import com.securebank.dto.CustomerDtos.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper: {@link Customer} (+ its {@code User}) -> {@link CustomerResponse}.
 * Flattens a couple of fields off the associated user.
 */
@Mapper
public interface CustomerMapper {

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "preferredLocale", source = "user.preferredLocale")
    CustomerResponse toResponse(Customer customer);
}
