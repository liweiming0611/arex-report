package com.arextest.web.model.dto.filesystem;

import lombok.Data;

@Data
public class AuthDto {
    private String authType;
    private Boolean authActive;
    private String token;
}
