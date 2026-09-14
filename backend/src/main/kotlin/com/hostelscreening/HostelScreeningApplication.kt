package com.hostelscreening

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling // Spring Batch 정기 데이터 갱신 트리거용 (3.8)
class HostelScreeningApplication

fun main(args: Array<String>) {
    runApplication<HostelScreeningApplication>(*args)
}
