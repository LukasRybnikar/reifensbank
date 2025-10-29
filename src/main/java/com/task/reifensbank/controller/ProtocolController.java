package com.task.reifensbank.controller;

import com.task.reifensbank.api.ProtocolsApi;
import com.task.reifensbank.model.Protocol;
import com.task.reifensbank.model.ProtocolCreate;
import com.task.reifensbank.model.ProtocolStateUpdate;
import com.task.reifensbank.model.ProtocolUpdate;
import com.task.reifensbank.usecase.ProtocolsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ProtocolController implements ProtocolsApi {

    private final ProtocolsAppService protocolsAppService;

    @Override
    public ResponseEntity<Protocol> protocolsCreate(ProtocolCreate protocolCreate) {
        return protocolsAppService.create(protocolCreate);
    }

    @Override
    public ResponseEntity<Protocol> protocolsGetById(UUID id) {
        return protocolsAppService.getById(id);
    }

    @Override
    public ResponseEntity<Protocol> protocolsUpdateAll(UUID id, ProtocolUpdate protocolUpdate) {
        return protocolsAppService.updateAll(id, protocolUpdate);
    }

    @Override
    public ResponseEntity<Protocol> protocolsUpdateState(UUID id, ProtocolStateUpdate protocolStateUpdate) {
        return protocolsAppService.updateState(id, protocolStateUpdate);
    }
}
