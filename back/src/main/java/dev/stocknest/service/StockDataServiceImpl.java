package dev.stocknest.service;

import dev.stocknest.component.StockDataComponent;
import dev.stocknest.model.dto.CustomerDTO;
import dev.stocknest.model.dto.StockDTO;
import dev.stocknest.model.entity.WatchStock;
import dev.stocknest.repository.StockDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockDataServiceImpl implements StockDataService {

    private final StockDataRepository stockDataRepository;
    private final StockDataComponent stockDataComponent;

    @Override
    public List<WatchStock> findAllStocks(CustomerDTO customerDto) {
        return stockDataRepository.findAllByCustomerId(customerDto.getId());
    }

    @Override
    public List<StockDTO> getStockChart(List<WatchStock> watchStocks) {
        return watchStocks.stream()
                .map(ws -> stockDataComponent.getStockChart(ws.getStockCode(), ws.getStockName()))
                .collect(Collectors.toList());
    }
}
