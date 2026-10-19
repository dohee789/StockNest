package dev.stocknest.service;

import dev.stocknest.model.dto.FuturesItemDTO;
import dev.stocknest.model.dto.FuturesOrderDTO;
import dev.stocknest.model.dto.FuturesPositionDTO;

import java.util.List;

public interface FuturesService {
    List<FuturesItemDTO> getAvailableFutures();
    FuturesItemDTO getFuturesDetail(String code);
    void openPosition(FuturesOrderDTO dto);
    void closePosition(String positionId, String customerId);
    List<FuturesPositionDTO> getPositions(String customerId);
    void checkLiquidations();
}
