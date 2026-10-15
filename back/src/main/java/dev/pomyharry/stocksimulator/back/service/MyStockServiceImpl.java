package dev.pomyharry.stocksimulator.back.service;

import dev.pomyharry.stocksimulator.back.component.StockDataComponent;
import dev.pomyharry.stocksimulator.back.exception.AccountNotFoundException;
import dev.pomyharry.stocksimulator.back.model.dto.MyStockDTO;
import dev.pomyharry.stocksimulator.back.model.dto.StockDTO;
import dev.pomyharry.stocksimulator.back.model.entity.*;
import dev.pomyharry.stocksimulator.back.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyStockServiceImpl implements MyStockService {

    private final MyStockRepository myStockRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final StockDataComponent stockDataComponent;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final NotificationService notificationService;

    private static final java.util.Set<String> MARKET_HOURS_EXEMPT = java.util.Set.of(
            "dohee789@naver.com",
            "doohee3773@naver.com",
            "dohee@naver.com"
    );

    private boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime nowTime = now.toLocalTime();
        return nowTime.isAfter(LocalTime.of(9, 0)) && nowTime.isBefore(LocalTime.of(16, 0));
    }

    private boolean isMarketHoursRequired(Customer customer) {
        return !MARKET_HOURS_EXEMPT.contains(customer.getEmail());
    }

    @Override
    @Transactional
    public void buyStock(MyStockDTO dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new AccountNotFoundException("고객을 찾을 수 없습니다."));

        if (!isMarketOpen() && isMarketHoursRequired(customer)) {
            throw new AccountNotFoundException("거래 시간이 아닙니다. (09:00 ~ 16:00)");
        }
        if (dto.getBuyPrice() <= 0 || dto.getAmount() <= 0) {
            throw new AccountNotFoundException("유효하지 않은 주문입니다. (가격 또는 수량 오류)");
        }

        Account account = accountRepository.findByCustomerId(dto.getCustomerId());
        if (account == null) {
            throw new AccountNotFoundException("계좌가 존재하지 않습니다.");
        }
        if (account.getDeposit() < dto.getBuyPrice()) {
            throw new AccountNotFoundException("잔액이 부족합니다.");
        }

        String stockName = (dto.getName() != null && !dto.getName().isBlank())
                ? dto.getName() : dto.getStockCode();

        MyStock myStock = myStockRepository.findByCustomerIdAndStockCode(dto.getCustomerId(), dto.getStockCode());
        if (myStock == null) {
            myStock = MyStock.builder()
                    .amount(dto.getAmount())
                    .totalBuyPrice(dto.getBuyPrice())
                    .customer(customer)
                    .stockCode(dto.getStockCode())
                    .stockName(stockName)
                    .build();
        } else {
            myStock.setAmount(myStock.getAmount() + dto.getAmount());
            myStock.setTotalBuyPrice(myStock.getTotalBuyPrice() + dto.getBuyPrice());
        }
        myStockRepository.save(myStock);

        account.setDeposit(account.getDeposit() - dto.getBuyPrice());
        accountRepository.save(account);

        long pricePerStock = dto.getBuyPrice() / dto.getAmount();
        tradeHistoryRepository.save(TradeHistory.builder()
                .tradeType("BUY")
                .stockCode(dto.getStockCode())
                .stockName(stockName)
                .amount(dto.getAmount())
                .pricePerStock(pricePerStock)
                .totalPrice(dto.getBuyPrice())
                .tradeTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .customer(customer)
                .build());

        String buyOrderType = (dto.getOrderType() != null) ? dto.getOrderType() : "MARKET";
        notificationService.sendTradeNotification(customer, "BUY", buyOrderType,
                stockName, dto.getAmount(), pricePerStock, dto.getBuyPrice());
    }

    @Override
    @Transactional
    public void sellStock(MyStockDTO dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new AccountNotFoundException("고객을 찾을 수 없습니다."));

        if (!isMarketOpen() && isMarketHoursRequired(customer)) {
            throw new AccountNotFoundException("거래 시간이 아닙니다. (09:00 ~ 16:00)");
        }

        Account account = accountRepository.findByCustomerId(dto.getCustomerId());
        if (account == null) {
            throw new AccountNotFoundException("계좌가 존재하지 않습니다.");
        }

        MyStock myStock = myStockRepository.findByCustomerIdAndStockCode(dto.getCustomerId(), dto.getStockCode());
        if (myStock == null) {
            throw new AccountNotFoundException("해당 종목을 보유하지 않습니다.");
        }
        if (myStock.getAmount() < dto.getAmount()) {
            throw new AccountNotFoundException("보유 수량이 부족합니다.");
        }

        long avgBuyPricePerShare = myStock.getTotalBuyPrice() / myStock.getAmount();
        long deductBuyPrice = avgBuyPricePerShare * dto.getAmount();

        myStock.setAmount(myStock.getAmount() - dto.getAmount());
        myStock.setTotalBuyPrice(myStock.getTotalBuyPrice() - deductBuyPrice);

        if (myStock.getAmount() == 0) {
            myStockRepository.delete(myStock);
        } else {
            myStockRepository.save(myStock);
        }

        account.setDeposit(account.getDeposit() + dto.getSellPrice());
        accountRepository.save(account);

        long sellPricePerStock = dto.getSellPrice() / dto.getAmount();
        String soldStockName = myStock.getStockName();
        tradeHistoryRepository.save(TradeHistory.builder()
                .tradeType("SELL")
                .stockCode(dto.getStockCode())
                .stockName(soldStockName)
                .amount(dto.getAmount())
                .pricePerStock(sellPricePerStock)
                .totalPrice(dto.getSellPrice())
                .tradeTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .customer(customer)
                .build());

        String sellOrderType = (dto.getOrderType() != null) ? dto.getOrderType() : "MARKET";
        notificationService.sendTradeNotification(customer, "SELL", sellOrderType,
                soldStockName, dto.getAmount(), sellPricePerStock, dto.getSellPrice());
    }

    @Override
    @Transactional
    public List<MyStockDTO> findAllMyStockByCustomerId(String customerId) {
        return myStockRepository.findAllByCustomerId(customerId).stream()
                .map(stock -> {
                    StockDTO sd = stockDataComponent.getStockChart(stock.getStockCode(), stock.getStockName());
                    long nowPrice = 0;
                    String resolvedName = stock.getStockName();
                    if (sd != null) {
                        if (sd.getPrice() != null) {
                            try {
                                nowPrice = Long.parseLong(sd.getPrice().replace(",", ""));
                            } catch (NumberFormatException ignored) {}
                        }
                        if (sd.getName() != null && !sd.getName().isBlank()) {
                            resolvedName = sd.getName();
                            // 이름이 비어있었던 기존 행은 DB에 저장
                            if (stock.getStockName() == null || stock.getStockName().isBlank()) {
                                stock.setStockName(resolvedName);
                                myStockRepository.save(stock);
                            }
                        }
                    }
                    return MyStockDTO.builder()
                            .totalBuyPrice(stock.getTotalBuyPrice())
                            .amount(stock.getAmount())
                            .stockCode(stock.getStockCode())
                            .name(resolvedName)
                            .nowPrice(nowPrice)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAllByCustomer(Customer customer) {
        myStockRepository.deleteAllByCustomer(customer);
    }

    @Override
    public void deleteByCustomerId(String customerId) {
        myStockRepository.deleteByCustomerId(customerId);
    }
}
