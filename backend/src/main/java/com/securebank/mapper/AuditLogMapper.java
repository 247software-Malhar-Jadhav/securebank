package com.securebank.mapper;

import com.securebank.domain.AuditLog;
import com.securebank.dto.AuditLogDtos.AuditLogResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper: {@link AuditLog} -> {@link AuditLogResponse}.
 */
@Mapper
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);

    List<AuditLogResponse> toResponseList(List<AuditLog> auditLogs);
}
