package dev.stocknest.service;

import dev.stocknest.model.dto.KakaoDTO;
import dev.stocknest.model.entity.Kakao;

public interface KakaoService {

	KakaoDTO getUserInfo(String access_Token);

	Kakao findById(String id);

	String getAccessToken (String authorize_code);
}