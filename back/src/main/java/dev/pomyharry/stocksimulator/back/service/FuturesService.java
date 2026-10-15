package dev.pomyharry.stocksimulator.back.service;

import dev.pomyharry.stocksimulator.back.model.dto.FuturesItemDTO;
import dev.pomyharry.stocksimulator.back.model.dto.FuturesOrderDTO;
import dev.pomyharry.stocksimulator.back.model.dto.FuturesPositionDTO;

import java.util.List;

public interface FuturesService {
    List<FuturesItemDTO> getAvailableFutures();
    FuturesItemDTO getFuturesDetail(String code);
    void openPosition(FuturesOrderDTO dto);
    void closePosition(String positionId, String customerId);
    List<FuturesPositionDTO> getPositions(String customerId);
    void checkLiquidations();
}
