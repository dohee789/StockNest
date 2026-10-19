package dev.stocknest.service;

import dev.stocknest.model.dto.TradeHistoryDTO;
import dev.stocknest.model.entity.TradeHistory;
import dev.stocknest.repository.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeHistoryServiceImpl implements TradeHistoryService {

    private final TradeHistoryRepository tradeHistoryRepository;

    @Override
    public void save(TradeHistory tradeHistory) {
        tradeHistoryRepository.save(tradeHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeHistoryDTO> findByCustomerId(String customerId) {
        return tradeHistoryRepository.findAllByCustomerIdOrderByTradeTimeDesc(customerId).stream()
                .map(h -> TradeHistoryDTO.builder()
                        .id(h.getId())
                        .tradeType(h.getTradeType())
                        .stockCode(h.getStockCode())
                        .stockName(h.getStockName())
                        .amount(h.getAmount())
                        .pricePerStock(h.getPricePerStock())
                        .totalPrice(h.getTotalPrice())
                        .tradeTime(h.getTradeTime())
                        .customerId(h.getCustomer().getId())
                        .build())
                .collect(Collectors.toList());
    }
}
