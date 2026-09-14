package com.hostelscreening.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * hostel-screening.cors.allowed-origins (콤마 구분 문자열)을 바인딩한다.
 * 프론트엔드(Vercel)와 백엔드(NCP)가 서로 다른 오리진이라
 * 이 설정이 없으면 브라우저가 API 응답을 차단한다.
 */
@ConfigurationProperties(prefix = "hostel-screening.cors")
class CorsProperties {
    var allowedOrigins: String = "http://localhost:3000"

    fun originList(): Array<String> =
        allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
}

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class CorsConfig(private val corsProperties: CorsProperties) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(*corsProperties.originList())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(3600)
    }
}
