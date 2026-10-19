package dev.stocknest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleDTO {
    private String date;   // "YYYY-MM-DD"
    private long open;
    private long high;
    private long low;
    private long close;
    private long volume;
}
