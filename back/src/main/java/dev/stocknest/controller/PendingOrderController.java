package dev.stocknest.controller;

import dev.stocknest.model.dto.PendingOrderDTO;
import dev.stocknest.service.PendingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/pending-order")
@RequiredArgsConstructor
public class PendingOrderController {

    private final PendingOrderService pendingOrderService;

    /** 지정가 주문 등록 */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody PendingOrderDTO dto) {
        try {
            pendingOrderService.create(dto);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 고객의 주문 목록 조회 */
    @PostMapping("/list")
    public ResponseEntity<?> list(@RequestBody Map<String, String> body) {
        try {
            String customerId = body.get("customerId");
            List<PendingOrderDTO> result = pendingOrderService.findByCustomerId(customerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 주문 취소 */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@RequestBody Map<String, String> body) {
        try {
            pendingOrderService.cancel(body.get("orderId"), body.get("customerId"));
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
