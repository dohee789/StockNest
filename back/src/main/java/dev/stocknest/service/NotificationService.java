package dev.stocknest.service;

import dev.stocknest.model.dto.NotificationDTO;
import dev.stocknest.model.entity.Customer;

import java.util.List;

public interface NotificationService {
    void sendTradeNotification(Customer customer, String tradeType, String orderType,
                               String stockName, int amount, long pricePerStock, long totalPrice);
    void sendDailySummaryNotification(Customer customer, int totalTrades, long totalBuyAmount, long totalSellAmount, long netPnl);
    void sendAiInsightNotification(Customer customer, String insight);
    List<NotificationDTO> getNotifications(String customerId);
    long getUnreadCount(String customerId);
    void markAllRead(String customerId);
    void deleteNotification(String notificationId);
    void deleteAllByCustomerId(String customerId);
}
