package dev.stocknest.controller;

import dev.stocknest.component.SiseRankComponent;
import dev.stocknest.model.dto.SiseRankDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/sise")
@RequiredArgsConstructor
public class SiseRankController {

    private final SiseRankComponent siseRankComponent;

    /** GET /sise/rising - 급등 top 20 */
    @GetMapping("/rising")
    public ResponseEntity<List<SiseRankDTO>> getRising() {
        return ResponseEntity.ok(siseRankComponent.getRising());
    }

    /** GET /sise/falling - 급락 top 20 */
    @GetMapping("/falling")
    public ResponseEntity<List<SiseRankDTO>> getFalling() {
        return ResponseEntity.ok(siseRankComponent.getFalling());
    }

    /** GET /sise/volume - 거래량 top 20 */
    @GetMapping("/volume")
    public ResponseEntity<List<SiseRankDTO>> getVolume() {
        return ResponseEntity.ok(siseRankComponent.getVolume());
    }
}
