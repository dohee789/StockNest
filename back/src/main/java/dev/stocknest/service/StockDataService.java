package dev.stocknest.service;

import java.util.List;

import dev.stocknest.model.dto.CustomerDTO;
import dev.stocknest.model.dto.StockDTO;
import dev.stocknest.model.entity.WatchStock;

public interface StockDataService {

    List<WatchStock> findAllStocks(CustomerDTO customer);

    public List<StockDTO> getStockChart(List<WatchStock> watchStocks);

}