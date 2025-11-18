package com.example.apigateway.config

import io.github.cdimascio.dotenv.dotenv
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.stereotype.Component

@Component
class DotenvConfig : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val dotenv = dotenv {
            ignoreIfMissing = true
        }

        val envMap = mutableMapOf<String, Any>()
        dotenv.entries().forEach { entry ->
            envMap[entry.key] = entry.value
        }

        applicationContext.environment.propertySources.addFirst(
            MapPropertySource("dotenvProperties", envMap)
        )
    }
}

