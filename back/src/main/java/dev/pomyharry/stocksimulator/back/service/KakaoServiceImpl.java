package dev.pomyharry.stocksimulator.back.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pomyharry.stocksimulator.back.exception.IdNotFoundException;
import dev.pomyharry.stocksimulator.back.model.dto.KakaoDTO;
import dev.pomyharry.stocksimulator.back.model.entity.Customer;
import dev.pomyharry.stocksimulator.back.model.entity.Kakao;
import dev.pomyharry.stocksimulator.back.repository.CustomerRepository;
import dev.pomyharry.stocksimulator.back.repository.KakaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class KakaoServiceImpl implements KakaoService {

    private final KakaoRepository kakaoRepository;
    private final CustomerRepository customerRepository;

    @Override
    public String getAccessToken(String authorizeCode) {
        String reqURL = "https://kauth.kakao.com/oauth/token";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(reqURL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                bw.write("grant_type=authorization_code"
                        + "&client_secret=5gqaSELk6uZgmrZ5SNKN3AasfVyppQYV"
                        + "&client_id=ef23cfa2f66fcc25924f54647f3f5460"
                        + "&redirect_uri=http://localhost:8090/kakaologin"
                        + "&code=" + authorizeCode);
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder err = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) err.append(line);
                    throw new RuntimeException("카카오 토큰 발급 실패 (HTTP " + status + "): " + err);
                }
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
                JsonElement element = JsonParser.parseString(result.toString());
                return element.getAsJsonObject().get("access_token").getAsString();
            }
        } catch (IOException e) {
            throw new RuntimeException("카카오 토큰 요청 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public Kakao findById(String id) {
        Kakao kakao = kakaoRepository.findByEmail(id);
        if (kakao == null) {
            throw new IdNotFoundException("Id cannot be found");
        }
        return kakao;
    }

    @Override
    public KakaoDTO getUserInfo(String accessToken) {
        String reqURL = "https://kapi.kakao.com/v2/user/me";
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(reqURL).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
            }

            JsonElement element = JsonParser.parseString(result.toString());
            JsonObject properties = element.getAsJsonObject().get("properties").getAsJsonObject();
            JsonObject kakaoAccount = element.getAsJsonObject().get("kakao_account").getAsJsonObject();
            String userId = element.getAsJsonObject().get("id").getAsString();
            String nickname = properties.get("nickname").getAsString();
            String email = kakaoAccount.get("email").getAsString();

            // 최초 로그인 시 DB에 저장
            Kakao existingKakao = kakaoRepository.findByEmail(email);
            if (existingKakao == null) {
                Customer customer = Customer.builder()
                        .id(userId)
                        .name(nickname)
                        .email(email)
                        .password("KAKAO")
                        .build();
                customerRepository.save(customer);

                Customer saved = customerRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("고객 저장 실패"));

                Kakao kakao = Kakao.builder()
                        .id(saved.getId())
                        .name(saved.getName())
                        .email(saved.getEmail())
                        .build();
                kakaoRepository.save(kakao);
            }

            Kakao info = kakaoRepository.findByEmail(email);
            info.setAccessToken(accessToken);
            kakaoRepository.save(info);
            return new KakaoDTO(info.getId(), info.getName(), info.getEmail());

        } catch (IOException e) {
            throw new RuntimeException("카카오 사용자 정보 조회 실패: " + e.getMessage(), e);
        }
    }
}
