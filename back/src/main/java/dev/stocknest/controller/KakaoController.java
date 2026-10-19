package dev.stocknest.controller;

import dev.stocknest.model.dto.KakaoDTO;
import dev.stocknest.service.KakaoService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
public class KakaoController {

    private final KakaoService kakaoService;

    @GetMapping("/kakaologin")
    public void kakaoLogin(@RequestParam(value = "code") String code, HttpServletResponse response) throws Exception {
        response.sendRedirect("http://localhost:3000/KakaoLogin/" + code);
    }

    @PostMapping("/afterlogin")
    public ResponseEntity<?> afterLogin(@RequestParam(value = "code") String code) {
        try {
            String accessToken = kakaoService.getAccessToken(code);
            KakaoDTO userInfo = kakaoService.getUserInfo(accessToken);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
