package dev.pomyharry.stocksimulator.back.service;

import dev.pomyharry.stocksimulator.back.model.dto.TradeHistoryDTO;
import dev.pomyharry.stocksimulator.back.model.entity.Customer;
import dev.pomyharry.stocksimulator.back.model.entity.TradeHistory;

import java.util.List;

public interface TradeHistoryService {
    void save(TradeHistory tradeHistory);
    List<TradeHistoryDTO> findByCustomerId(String customerId);
}
