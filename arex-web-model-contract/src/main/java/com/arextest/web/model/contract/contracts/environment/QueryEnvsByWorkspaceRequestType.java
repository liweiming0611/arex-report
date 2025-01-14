package com.arextest.web.model.contract.contracts.environment;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class QueryEnvsByWorkspaceRequestType {
    @NotBlank(message = "Workspace id cannot be empty")
    private String workspaceId;
}
