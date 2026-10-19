package dev.stocknest.service;

import dev.stocknest.model.dto.PendingOrderDTO;

import java.util.List;

public interface PendingOrderService {

    void create(PendingOrderDTO dto);

    void cancel(String orderId, String customerId);

    List<PendingOrderDTO> findByCustomerId(String customerId);

    void checkAndExecuteAll();
}
