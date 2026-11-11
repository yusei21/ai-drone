package com.sloth.registerapp.core.observability

import com.sloth.registerapp.BuildConfig
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel

object AppObservability {

    fun configureAppScope() {
        Sentry.configureScope { scope ->
            scope.setTag("app.module", "android")
            scope.setTag("build.type", BuildConfig.BUILD_TYPE)
            scope.setTag("version.name", BuildConfig.VERSION_NAME)
            scope.setExtra("version_code", BuildConfig.VERSION_CODE.toString())
        }
    }

    fun screenOpened(screenName: String, extras: Map<String, Any?> = emptyMap()) {
        Sentry.configureScope { scope ->
            scope.setTag("screen", screenName)
        }
        breadcrumb(
            category = "navigation",
            message = "Screen opened: $screenName",
            data = extras + ("screen" to screenName)
        )
    }

    fun breadcrumb(
        category: String,
        message: String,
        level: SentryLevel = SentryLevel.INFO,
        data: Map<String, Any?> = emptyMap()
    ) {
        val breadcrumb = Breadcrumb().apply {
            this.category = category
            this.message = message
            this.level = level
            data.forEach { (key, value) ->
                value?.let { setData(key, it) }
            }
        }
        Sentry.addBreadcrumb(breadcrumb)
    }

    fun captureException(
        throwable: Throwable,
        area: String,
        operation: String,
        level: SentryLevel = SentryLevel.ERROR,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, Any?> = emptyMap()
    ) {
        Sentry.withScope { scope ->
            scope.setLevel(level)
            scope.setTag("area", area)
            scope.setTag("operation", operation)
            tags.forEach { (key, value) -> scope.setTag(key, value) }
            extras.forEach { (key, value) -> value?.let { scope.setExtra(key, it.toString()) } }
            Sentry.captureException(throwable)
        }
    }

    fun captureMessage(
        message: String,
        area: String,
        operation: String,
        level: SentryLevel = SentryLevel.WARNING,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, Any?> = emptyMap()
    ) {
        Sentry.withScope { scope ->
            scope.setLevel(level)
            scope.setTag("area", area)
            scope.setTag("operation", operation)
            tags.forEach { (key, value) -> scope.setTag(key, value) }
            extras.forEach { (key, value) -> value?.let { scope.setExtra(key, it.toString()) } }
            Sentry.captureMessage(message, level)
        }
    }
}
