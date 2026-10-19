package dev.stocknest.service;

import dev.stocknest.model.dto.TradeHistoryDTO;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.model.entity.TradeHistory;

import java.util.List;

public interface TradeHistoryService {
    void save(TradeHistory tradeHistory);
    List<TradeHistoryDTO> findByCustomerId(String customerId);
}
