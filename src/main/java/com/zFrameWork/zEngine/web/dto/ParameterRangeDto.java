package com.zFrameWork.zEngine.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterRangeDto {
    private String name;
    private BigDecimal start;
    private BigDecimal end;
    private BigDecimal step;
}
