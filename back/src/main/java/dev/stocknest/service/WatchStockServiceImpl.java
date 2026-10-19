package dev.stocknest.service;

import dev.stocknest.exception.DuplicationException;
import dev.stocknest.model.dto.WatchStockDTO;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.model.entity.WatchStock;
import dev.stocknest.repository.WatchStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchStockServiceImpl implements WatchStockService {

    private final WatchStockRepository watchStockRepository;

    @Override
    public WatchStock createWatchList(WatchStock stock) {
        WatchStock existing = watchStockRepository.findByStockCodeAndCustomerId(
                stock.getStockCode(), stock.getCustomer().getId());
        if (existing != null) {
            throw new DuplicationException("이미 등록된 관심종목입니다.");
        }
        return watchStockRepository.save(stock);
    }

    @Override
    public void deleteAllWatchList(Customer customer) {
        watchStockRepository.deleteAllByCustomer(customer);
    }

    @Override
    public List<WatchStockDTO> findAllWatchStockByCustomerId(String customerId) {
        return watchStockRepository.findAllByCustomerId(customerId).stream()
                .map(r -> WatchStockDTO.builder()
                        .index(r.getIndex())
                        .code(r.getStockCode())
                        .name(r.getStockName())
                        .customerId(r.getCustomer().getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByStockCode(String code) {
        watchStockRepository.deleteByStockCode(code);
    }
}
