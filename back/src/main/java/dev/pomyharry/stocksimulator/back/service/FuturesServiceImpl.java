package dev.pomyharry.stocksimulator.back.service;

import dev.pomyharry.stocksimulator.back.component.FuturesDataComponent;
import dev.pomyharry.stocksimulator.back.model.dto.FuturesItemDTO;
import dev.pomyharry.stocksimulator.back.model.dto.FuturesOrderDTO;
import dev.pomyharry.stocksimulator.back.model.dto.FuturesPositionDTO;
import dev.pomyharry.stocksimulator.back.model.entity.Account;
import dev.pomyharry.stocksimulator.back.model.entity.Customer;
import dev.pomyharry.stocksimulator.back.model.entity.FuturesPosition;
import dev.pomyharry.stocksimulator.back.repository.AccountRepository;
import dev.pomyharry.stocksimulator.back.repository.CustomerRepository;
import dev.pomyharry.stocksimulator.back.repository.FuturesPositionRepository;
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
public class FuturesServiceImpl implements FuturesService {

    private final FuturesPositionRepository positionRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final FuturesDataComponent futuresDataComponent;

    @Override
    public List<FuturesItemDTO> getAvailableFutures() {
        return futuresDataComponent.getAllPrices();
    }

    @Override
    public FuturesItemDTO getFuturesDetail(String code) {
        return futuresDataComponent.getPrice(code);
    }

    @Override
    @Transactional
    public void openPosition(FuturesOrderDTO dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("고객을 찾을 수 없습니다."));

        if (dto.getMarginAmount() <= 0) {
            throw new RuntimeException("증거금은 0원보다 커야 합니다.");
        }
        if (!List.of(2, 5, 10, 20).contains(dto.getLeverage())) {
            throw new RuntimeException("레버리지는 2, 5, 10, 20 중 선택해야 합니다.");
        }
        if (!List.of("LONG", "SHORT").contains(dto.getDirection())) {
            throw new RuntimeException("방향은 LONG 또는 SHORT 이어야 합니다.");
        }

        Account account = accountRepository.findByCustomerId(dto.getCustomerId());
        if (account == null) throw new RuntimeException("계좌가 없습니다. 계좌를 먼저 개설하세요.");
        if (account.getDeposit() < dto.getMarginAmount()) {
            throw new RuntimeException("잔액이 부족합니다. (예수금: " + account.getDeposit() + "원)");
        }

        FuturesItemDTO priceData = futuresDataComponent.getPrice(dto.getFuturesCode());
        if (priceData == null) throw new RuntimeException("지원하지 않는 선물 종목입니다.");
        if (!priceData.isAvailable() || priceData.getCurrentPrice() == null) {
            throw new RuntimeException("현재가 조회에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        positionRepository.save(FuturesPosition.builder()
                .customer(customer)
                .futuresCode(dto.getFuturesCode())
                .futuresName(priceData.getName())
                .direction(dto.getDirection())
                .marginAmount(dto.getMarginAmount())
                .leverage(dto.getLeverage())
                .entryPrice(priceData.getCurrentPrice())
                .status("OPEN")
                .openedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build());

        account.setDeposit(account.getDeposit() - dto.getMarginAmount());
        accountRepository.save(account);

        log.info("선물 포지션 오픈: {} {} {}x 증거금{}원 @{}",
                dto.getDirection(), dto.getFuturesCode(), dto.getLeverage(),
                dto.getMarginAmount(), priceData.getCurrentPrice());
    }

    @Override
    @Transactional
    public void closePosition(String positionId, String customerId) {
        FuturesPosition pos = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("포지션을 찾을 수 없습니다."));

        if (!pos.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("권한이 없습니다.");
        }
        if (!"OPEN".equals(pos.getStatus())) {
            throw new RuntimeException("이미 청산된 포지션입니다.");
        }

        FuturesItemDTO priceData = futuresDataComponent.getPrice(pos.getFuturesCode());
        if (priceData == null || !priceData.isAvailable() || priceData.getCurrentPrice() == null) {
            throw new RuntimeException("현재가 조회에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        long pnl = calcPnl(pos, priceData.getCurrentPrice());
        long returnAmount = Math.max(0, pos.getMarginAmount() + pnl);

        pos.setStatus("CLOSED");
        pos.setClosedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        pos.setRealizedPnl(pnl);
        positionRepository.save(pos);

        Account account = accountRepository.findByCustomerId(customerId);
        account.setDeposit(account.getDeposit() + returnAmount);
        accountRepository.save(account);

        log.info("선물 포지션 청산: {} {} PnL={}원", pos.getDirection(), pos.getFuturesCode(), pnl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuturesPositionDTO> getPositions(String customerId) {
        return positionRepository.findAllByCustomerId(customerId).stream()
                .map(p -> {
                    FuturesItemDTO priceData = "OPEN".equals(p.getStatus())
                            ? futuresDataComponent.getPrice(p.getFuturesCode()) : null;

                    Double currentPrice = null;
                    Long unrealizedPnl = null;
                    double unrealizedPnlRate = 0;

                    if (priceData != null && priceData.isAvailable()) {
                        currentPrice = priceData.getCurrentPrice();
                        unrealizedPnl = calcPnl(p, currentPrice);
                        unrealizedPnlRate = p.getMarginAmount() != 0
                                ? (double) unrealizedPnl / p.getMarginAmount() * 100 : 0;
                    }

                    String currency = FuturesDataComponent.CATALOG.stream()
                            .filter(c -> c.getCode().equals(p.getFuturesCode()))
                            .map(FuturesItemDTO::getCurrency)
                            .findFirst().orElse("USD");

                    return FuturesPositionDTO.builder()
                            .id(p.getId())
                            .futuresCode(p.getFuturesCode())
                            .futuresName(p.getFuturesName())
                            .direction(p.getDirection())
                            .marginAmount(p.getMarginAmount())
                            .leverage(p.getLeverage())
                            .entryPrice(p.getEntryPrice())
                            .currentPrice(currentPrice)
                            .currency(currency)
                            .unrealizedPnl(unrealizedPnl)
                            .unrealizedPnlRate(unrealizedPnlRate)
                            .status(p.getStatus())
                            .openedAt(p.getOpenedAt())
                            .closedAt(p.getClosedAt())
                            .realizedPnl(p.getRealizedPnl())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void checkLiquidations() {
        List<FuturesPosition> opens = positionRepository.findAllOpen();
        for (FuturesPosition pos : opens) {
            try {
                FuturesItemDTO priceData = futuresDataComponent.getPrice(pos.getFuturesCode());
                if (priceData == null || !priceData.isAvailable()) continue;

                long pnl = calcPnl(pos, priceData.getCurrentPrice());

                // 증거금 전부 소진 시 강제청산
                if (pnl <= -pos.getMarginAmount()) {
                    pos.setStatus("LIQUIDATED");
                    pos.setClosedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
                    pos.setRealizedPnl(-pos.getMarginAmount()); // 전액 손실
                    positionRepository.save(pos);
                    log.warn("선물 강제청산: {} {} 증거금={}원", pos.getDirection(), pos.getFuturesCode(), pos.getMarginAmount());
                    // 반환금 없음 (deposit 변경 불필요)
                }
            } catch (Exception e) {
                log.warn("강제청산 체크 실패 [{}]: {}", pos.getId(), e.getMessage());
            }
        }
    }

    // ----------------------------------------------------------------
    // 내부 계산
    // ----------------------------------------------------------------

    /**
     * 평가손익 (KRW) = 증거금 × 레버리지 × 가격변화율 × 방향(±1)
     */
    private long calcPnl(FuturesPosition pos, double currentPrice) {
        double priceChangePct = (currentPrice - pos.getEntryPrice()) / pos.getEntryPrice();
        double direction = "LONG".equals(pos.getDirection()) ? 1.0 : -1.0;
        return Math.round(pos.getMarginAmount() * pos.getLeverage() * priceChangePct * direction);
    }
}
