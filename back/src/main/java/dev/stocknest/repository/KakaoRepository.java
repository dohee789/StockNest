package dev.stocknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import dev.stocknest.model.entity.Kakao;

public interface KakaoRepository extends JpaRepository<Kakao, String> {

    Kakao findByEmail(String email);

}
