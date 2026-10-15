package dev.pomyharry.stocksimulator.back.service;

import dev.pomyharry.stocksimulator.back.model.dto.PendingOrderDTO;

import java.util.List;

public interface PendingOrderService {

    void create(PendingOrderDTO dto);

    void cancel(String orderId, String customerId);

    List<PendingOrderDTO> findByCustomerId(String customerId);

    void checkAndExecuteAll();
}
