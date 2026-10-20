package dev.stocknest.service;

import dev.stocknest.model.dto.TradeHistoryDTO;
import dev.stocknest.model.entity.MyStock;
import dev.stocknest.model.entity.TradeHistory;
import dev.stocknest.repository.MyStockRepository;
import dev.stocknest.repository.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeHistoryServiceImpl implements TradeHistoryService {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final MyStockRepository myStockRepository;

    @Override
    public void save(TradeHistory tradeHistory) {
        tradeHistoryRepository.save(tradeHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeHistoryDTO> findByCustomerId(String customerId) {
        List<TradeHistory> histories = tradeHistoryRepository
                .findAllByCustomerIdOrderByTradeTimeDesc(customerId);

        // stockName이 종목코드(숫자)로 저장된 기존 데이터 보정용
        Map<String, String> nameCache = myStockRepository.findAllByCustomerId(customerId)
                .stream()
                .collect(Collectors.toMap(
                        MyStock::getStockCode,
                        MyStock::getStockName,
                        (a, b) -> a
                ));

        return histories.stream()
                .map(h -> {
                    String name = h.getStockName();
                    // stockName이 stockCode와 동일하면(숫자만) 보정
                    if (name != null && name.equals(h.getStockCode())) {
                        String realName = nameCache.get(h.getStockCode());
                        if (realName != null && !realName.isBlank()) {
                            name = realName;
                        }
                    }
                    return TradeHistoryDTO.builder()
                            .id(h.getId())
                            .tradeType(h.getTradeType())
                            .stockCode(h.getStockCode())
                            .stockName(name)
                            .amount(h.getAmount())
                            .pricePerStock(h.getPricePerStock())
                            .totalPrice(h.getTotalPrice())
                            .tradeTime(h.getTradeTime())
                            .customerId(h.getCustomer().getId())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
