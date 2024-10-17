// RequestDTO.java
package com.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Request {
    private int startRow;
    private int endRow;
    private List<SortModel> sortModel;
    private Map<String, FilterModel> filterModel;
}

