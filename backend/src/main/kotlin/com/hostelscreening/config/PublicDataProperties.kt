package com.hostelscreening.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * hostel-screening.public-data.* 바인딩 — 건축HUB(대장/인허가), 토지이용계획 API의
 * base-url과 인증키. 세 API 모두 공공데이터포털(data.go.kr) 계정 하나로 발급받은
 * 동일한 일반 인증키(PUBLIC_DATA_SERVICE_KEY)를 공유하는 것이 보통이다(포털 정책).
 */
@ConfigurationProperties(prefix = "hostel-screening.public-data")
class PublicDataProperties {
    var buildingHub: Endpoint = Endpoint()
    var landUse: Endpoint = Endpoint()
    var permitInfo: Endpoint = Endpoint()

    class Endpoint {
        var baseUrl: String = ""
        var serviceKey: String = ""
    }
}

/**
 * hostel-screening.kakao.* 바인딩 — 서버 쪽에서 쓰는 카카오 로컬(Local) API REST 키.
 * 프론트엔드 지도 렌더링용 JavaScript 키와는 다른 키다(NEXT_PUBLIC_KAKAO_MAP_JS_KEY).
 */
@ConfigurationProperties(prefix = "hostel-screening.kakao")
class KakaoProperties {
    var localApi: Endpoint = Endpoint()

    class Endpoint {
        var baseUrl: String = ""
        var restApiKey: String = ""
    }
}
