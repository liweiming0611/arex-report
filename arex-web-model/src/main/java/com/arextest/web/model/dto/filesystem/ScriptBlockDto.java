package com.arextest.web.model.dto.filesystem;

import lombok.Data;

/**
 * @author b_yu
 * @since 2022/12/8
 */
@Data
public class ScriptBlockDto {
    private String type;
    private String icon;
    private String label;
    private String value;
    private Boolean disabled;
}
