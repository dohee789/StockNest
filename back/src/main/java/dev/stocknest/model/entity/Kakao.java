package dev.stocknest.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "kakao")
public class Kakao {

    @Id
    @Column(name = "kakao_id", nullable = false)
    private String id;

    @Column(name = "kakao_name")
    private String name;

    @Column(name = "kakao_email")
    private String email;

    @Column(name = "access_token", length = 512)
    private String accessToken;
}
