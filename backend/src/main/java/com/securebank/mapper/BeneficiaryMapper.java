package com.securebank.mapper;

import com.securebank.domain.Beneficiary;
import com.securebank.dto.BeneficiaryDtos.BeneficiaryResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper: {@link Beneficiary} -> {@link BeneficiaryResponse}.
 */
@Mapper
public interface BeneficiaryMapper {

    BeneficiaryResponse toResponse(Beneficiary beneficiary);

    List<BeneficiaryResponse> toResponseList(List<Beneficiary> beneficiaries);
}
