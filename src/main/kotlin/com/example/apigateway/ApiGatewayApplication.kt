package com.example.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File

@SpringBootApplication
class ApiGatewayApplication

fun main(args: Array<String>) {
    // 프로젝트 루트의 .env 파일을 읽어 시스템 프로퍼티로 주입합니다.
    // 이미 System property나 환경변수로 설정된 키는 덮어쓰지 않습니다.
    try {
        val envFile = File(".env")
        if (envFile.exists()) {
            envFile.forEachLine { raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachLine

                // 허용되는 형식: KEY=VALUE 또는 export KEY=VALUE
                val cleaned = if (line.startsWith("export ")) line.removePrefix("export ").trim() else line
                val parts = cleaned.split("=", limit = 2)
                if (parts.size != 2) return@forEachLine

                val key = parts[0].trim()
                var value = parts[1].trim()

                // 값이 따옴표로 감싸여 있으면 제거
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length - 1)
                }

                // 이미 환경변수 또는 시스템 프로퍼티에 존재하면 덮어쓰지 않음
                val alreadySet = System.getProperty(key) != null || System.getenv(key) != null
                if (!alreadySet) {
                    System.setProperty(key, value)
                }
            }
        }
    } catch (e: Exception) {
        // .env 로드 실패 시에도 애플리케이션은 계속 실행되도록 로그 출력
        println("Warning: failed to load .env file: ${e.message}")
    }

    runApplication<ApiGatewayApplication>(*args)
}
