package com.hostelscreening.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * 공공데이터포털/카카오 API 전용 WebClient 빈 — 어댑터별로 base-url이 다르므로 이름을 붙여 구분한다.
 * 도메인 계층(ScreeningEngine 등)은 이 빈들을 전혀 모른다 — adapter/publicdata, adapter/geocoding
 * 패키지의 어댑터만 주입받는다 (01-architecture.md 포트-어댑터 원칙).
 */
@Configuration
@EnableConfigurationProperties(PublicDataProperties::class, KakaoProperties::class)
class PublicDataWebClientConfig(
    private val publicDataProperties: PublicDataProperties,
    private val kakaoProperties: KakaoProperties,
) {

    @Bean
    fun buildingHubWebClient(): WebClient =
        WebClient.builder().baseUrl(publicDataProperties.buildingHub.baseUrl).build()

    @Bean
    fun permitInfoWebClient(): WebClient =
        WebClient.builder().baseUrl(publicDataProperties.permitInfo.baseUrl).build()

    @Bean
    fun kakaoLocalWebClient(): WebClient =
        WebClient.builder()
            .baseUrl(kakaoProperties.localApi.baseUrl)
            .defaultHeader("Authorization", "KakaoAK ${kakaoProperties.localApi.restApiKey}")
            .build()
}
