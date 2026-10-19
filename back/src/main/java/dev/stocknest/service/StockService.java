package dev.stocknest.service;

import java.util.List;
import dev.stocknest.model.entity.Stock;

public interface StockService {

    List<Stock> findAllStocks();

    Stock findByCode(String code);

    List<Stock> searchByName(String name);
}
