package dev.pomyharry.stocksimulator.back.service;

import dev.pomyharry.stocksimulator.back.component.StockDataComponent;
import dev.pomyharry.stocksimulator.back.model.dto.CustomerDTO;
import dev.pomyharry.stocksimulator.back.model.dto.StockDTO;
import dev.pomyharry.stocksimulator.back.model.entity.WatchStock;
import dev.pomyharry.stocksimulator.back.repository.StockDataRepository;
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
