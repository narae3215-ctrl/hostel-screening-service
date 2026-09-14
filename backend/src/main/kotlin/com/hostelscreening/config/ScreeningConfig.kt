package com.hostelscreening.config

import com.hostelscreening.domain.screening.ScreeningEngine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** 순수 도메인 클래스인 ScreeningEngine을 Spring 컨테이너에 빈으로 등록한다. */
@Configuration
class ScreeningConfig {

    @Bean
    fun screeningEngine(): ScreeningEngine = ScreeningEngine()
}
