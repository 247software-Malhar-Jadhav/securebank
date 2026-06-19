package com.securebank.mapper;

import com.securebank.domain.Transaction;
import com.securebank.dto.TransactionDtos.TransactionResponse;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper: {@link Transaction} entity -> {@link TransactionResponse}.
 *
 * <p>The entity holds associated {@code Account} objects; the DTO only needs
 * their ids, so we map {@code account.id -> accountId} explicitly. MapStruct
 * generates null-safe navigation for the optional counterparty.</p>
 */
@Mapper
public interface TransactionMapper {

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "counterpartyAccountId", source = "counterpartyAccount.id")
    TransactionResponse toResponse(Transaction transaction);

    List<TransactionResponse> toResponseList(List<Transaction> transactions);
}
