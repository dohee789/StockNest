package dev.stocknest.service;

import dev.stocknest.component.StockDataComponent;
import dev.stocknest.model.dto.MyStockDTO;
import dev.stocknest.model.dto.PendingOrderDTO;
import dev.stocknest.model.dto.StockDTO;
import dev.stocknest.model.entity.Customer;
import dev.stocknest.model.entity.PendingOrder;
import dev.stocknest.repository.CustomerRepository;
import dev.stocknest.repository.PendingOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingOrderServiceImpl implements PendingOrderService {

    private final PendingOrderRepository pendingOrderRepository;
    private final CustomerRepository customerRepository;
    private final MyStockService myStockService;
    private final StockDataComponent stockDataComponent;

    @Override
    @Transactional
    public void create(PendingOrderDTO dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));

        String stockName = (dto.getStockName() != null && !dto.getStockName().isBlank())
                ? dto.getStockName() : dto.getStockCode();

        PendingOrder order = PendingOrder.builder()
                .stockCode(dto.getStockCode())
                .stockName(stockName)
                .orderType(dto.getOrderType())
                .conditionType(dto.getConditionType())
                .targetPrice(dto.getTargetPrice())
                .amount(dto.getAmount())
                .status("PENDING")
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .customer(customer)
                .build();

        pendingOrderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancel(String orderId, String customerId) {
        PendingOrder order = pendingOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("권한이 없습니다.");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("취소 가능한 주문이 아닙니다.");
        }
        order.setStatus("CANCELLED");
        pendingOrderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingOrderDTO> findByCustomerId(String customerId) {
        return pendingOrderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(o -> PendingOrderDTO.builder()
                        .id(o.getId())
                        .stockCode(o.getStockCode())
                        .stockName(o.getStockName())
                        .orderType(o.getOrderType())
                        .conditionType(o.getConditionType())
                        .targetPrice(o.getTargetPrice())
                        .amount(o.getAmount())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .executedAt(o.getExecutedAt())
                        .customerId(o.getCustomer().getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 스케줄러 호출용: 모든 PENDING 주문에 대해 조건 체결 여부를 확인하고 실행
     * @Transactional 없음 → 각 주문 처리는 myStockService (REQUIRED) 가 독립 트랜잭션으로 처리
     */
    @Override
    public void checkAndExecuteAll() {
        List<PendingOrder> pending = pendingOrderRepository.findAllPendingWithCustomer();
        if (pending.isEmpty()) return;

        for (PendingOrder order : pending) {
            try {
                StockDTO priceData = stockDataComponent.getStockChart(order.getStockCode(), order.getStockName());
                if (priceData == null || priceData.getPrice() == null) continue;

                long currentPrice = Long.parseLong(priceData.getPrice().replace(",", ""));
                boolean shouldExecute = false;

                if ("PRICE_BELOW".equals(order.getConditionType()) && currentPrice <= order.getTargetPrice()) {
                    shouldExecute = true;
                } else if ("PRICE_ABOVE".equals(order.getConditionType()) && currentPrice >= order.getTargetPrice()) {
                    shouldExecute = true;
                }

                if (!shouldExecute) continue;

                String customerId = order.getCustomer().getId();
                int amount = order.getAmount();

                MyStockDTO tradeDto = MyStockDTO.builder()
                        .customerId(customerId)
                        .stockCode(order.getStockCode())
                        .name(order.getStockName())
                        .amount(amount)
                        .orderType("LIMIT")
                        .build();

                if ("BUY".equals(order.getOrderType())) {
                    tradeDto.setBuyPrice(currentPrice * amount);
                    myStockService.buyStock(tradeDto);
                } else {
                    tradeDto.setSellPrice(currentPrice * amount);
                    myStockService.sellStock(tradeDto);
                }

                // 체결 처리 (별도 트랜잭션)
                updateOrderExecuted(order.getId());
                log.info("지정가 주문 체결: {} {} {}주 @ {}원", order.getOrderType(), order.getStockCode(), amount, currentPrice);

            } catch (Exception e) {
                log.warn("지정가 주문 처리 실패 [{}]: {}", order.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    protected void updateOrderExecuted(String orderId) {
        pendingOrderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus("EXECUTED");
            o.setExecutedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            pendingOrderRepository.save(o);
        });
    }
}
