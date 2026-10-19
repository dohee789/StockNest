package dev.stocknest.service;

import dev.stocknest.exception.IdNotFoundException;
import dev.stocknest.model.entity.Stock;
import dev.stocknest.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;

    @Override
    public List<Stock> findAllStocks() {
        return stockRepository.findAll();
    }

    @Override
    public Stock findByCode(String code) {
        return stockRepository.findByCode(code)
                .orElseThrow(() -> new IdNotFoundException("존재하지 않는 종목입니다: " + code));
    }

    @Override
    public List<Stock> searchByName(String name) {
        return stockRepository.findByNameContainingIgnoreCase(name);
    }
}
