package com.securebank.mapper;

import com.securebank.domain.Account;
import com.securebank.dto.AccountDtos.AccountResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper: {@link Account} entity -> {@link AccountResponse} DTO.
 *
 * <p>MapStruct generates the implementation at compile time (no reflection),
 * keeping the controller/service free of hand-written field copying and honouring
 * the "never expose entities" rule. {@code componentModel = spring} (set globally
 * via the compiler arg) makes the generated mapper an injectable Spring bean.</p>
 */
@Mapper
public interface AccountMapper {

    /** Field names match one-to-one, so MapStruct needs no explicit @Mapping. */
    AccountResponse toResponse(Account account);

    List<AccountResponse> toResponseList(List<Account> accounts);
}
