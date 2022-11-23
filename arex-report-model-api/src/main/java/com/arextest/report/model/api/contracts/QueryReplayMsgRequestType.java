package com.arextest.report.model.api.contracts;

import lombok.Data;

import javax.validation.constraints.NotBlank;


@Data
public class QueryReplayMsgRequestType {
    @NotBlank(message = "Replay Id cannot be empty")
    private String id;
}
