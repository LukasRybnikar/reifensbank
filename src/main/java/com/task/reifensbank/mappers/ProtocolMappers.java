package com.task.reifensbank.mappers;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.entity.Protocol;
import com.task.reifensbank.enums.ProtocolStatusEnum;
import com.task.reifensbank.model.ProtocolState;
import lombok.experimental.UtilityClass;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class ProtocolMappers {

    public com.task.reifensbank.model.Protocol toModel(Protocol entity) {
        if (entity == null) return null;

        com.task.reifensbank.model.Protocol model = new com.task.reifensbank.model.Protocol();
        model.setId(entity.getPublicId());
        model.setState(toStateModel(entity.getStatus()));

        Set<UUID> docIds = entity.getDocuments() != null
                ? entity.getDocuments().stream()
                .map(Document::getPublicId)
                .collect(Collectors.toSet())
                : Set.of();
        model.setDocumentIds(docIds.stream().toList());

        return model;
    }

    private ProtocolState toStateModel(ProtocolStatusEnum status) {
        if (status == null) return null;
        try {
            return ProtocolState.valueOf(status.name());
        } catch (IllegalArgumentException ex) {
            return ProtocolState.NEW;
        }
    }
}
