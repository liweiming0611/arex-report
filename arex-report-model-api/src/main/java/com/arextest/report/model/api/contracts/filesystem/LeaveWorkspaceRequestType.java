package com.arextest.report.model.api.contracts.filesystem;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LeaveWorkspaceRequestType {
    private String userName;
    @NotBlank(message = "WorkspaceId cannot be empty")
    private String workspaceId;
}
