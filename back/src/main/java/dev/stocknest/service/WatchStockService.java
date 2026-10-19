package dev.stocknest.service;

import java.util.List;

import dev.stocknest.model.dto.WatchStockDTO;
import dev.stocknest.model.entity.WatchStock;
import dev.stocknest.model.entity.Customer;

public interface WatchStockService {

    WatchStock createWatchList(WatchStock stock);

    void deleteAllWatchList(Customer customer);

    List<WatchStockDTO> findAllWatchStockByCustomerId(String customerId);

    void deleteByStockCode(String code);

}
