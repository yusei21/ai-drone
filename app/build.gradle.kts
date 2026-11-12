import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")

    id("io.sentry.android.gradle") version "6.4.0"
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { this.load(it) }
    }
}

fun resolveSecret(key: String): String {
    return (
        localProperties.getProperty(key)
            ?: providers.gradleProperty(key).orNull
            ?: System.getenv(key)
            ?: ""
    ).trim().replace("\"", "\\\"")
}

val weatherApiKey = resolveSecret("WEATHER_API_KEY")

val mapboxAccessToken = resolveSecret("MAPBOX_ACCESS_TOKEN")
val sentryDsn = resolveSecret("SENTRY_DSN")
    .ifBlank { "https://e6a4ae2ec66c934386fcdd581d837f3b@o4511248860774400.ingest.us.sentry.io/4511248870408192" }

android {
    namespace = "com.sloth.registerapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sloth.registerapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
        buildConfigField("boolean", "ALLOW_PUBLIC_FLIGHT_OPS", "true")
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxAccessToken\"")
        resValue("string", "mapbox_access_token", mapboxAccessToken.ifBlank { "" })
        manifestPlaceholders["sentryDsn"] = sentryDsn

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            manifestPlaceholders["sentryTracesSampleRate"] = "1.0"
            manifestPlaceholders["sentryProfilesSampleRate"] = "0.0"
        }
        release {
            isMinifyEnabled = false
            manifestPlaceholders["sentryTracesSampleRate"] = "0.2"
            manifestPlaceholders["sentryProfilesSampleRate"] = "0.0"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    androidResources {
        // TFLite assets must stay uncompressed so AssetFileDescriptor can mmap them.
        noCompress += "tflite"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/rxjava.properties"
        }
        jniLibs {
            keepDebugSymbols.add("**/libdjivideo.so")
            keepDebugSymbols.add("**/libSDKRelativeJNI.so")
            keepDebugSymbols.add("**/libFlyForbid.so")
            keepDebugSymbols.add("**/libduml_vision_bokeh.so")
            keepDebugSymbols.add("**/libyuv2.so")
            keepDebugSymbols.add("**/libGroudStation.so")
            keepDebugSymbols.add("**/libFRCorkscrew.so")
            keepDebugSymbols.add("**/libUpgradeVerify.so")
            keepDebugSymbols.add("**/libFR.so")
            keepDebugSymbols.add("**/libDJIFlySafeCore.so")
            keepDebugSymbols.add("**/libdjifs_jni.so")
            keepDebugSymbols.add("**/libsfjni.so")
        }
    }
}

dependencies {

    //Gemini Dependencies
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.camera.view)
    implementation(libs.litert.gpu)
    implementation(libs.androidx.compose.foundation)

    // Room Database
    val roomVersion = "2.8.3"

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Gson para converter FloatArray em String
    implementation("com.google.code.gson:gson:2.10.1")

    // Compose BOM (versão compatível)
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)

    // Compose libraries
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")


    implementation(libs.litert)

    val camerax_version = "1.5.1"
    implementation(libs.dji.sdk)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.paging.common.android)
    implementation(libs.androidx.games.activity)
    implementation(libs.androidx.camera.lifecycle)
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation(libs.androidx.camera.core)
    compileOnly(libs.dji.sdk.provided)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Accompanist para permissões
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Paging 3 para Runtime (Core)
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    // Paging 3 para integração com Jetpack Compose
    implementation("androidx.paging:paging-compose:3.3.0")
    // Coil para carregamento de imagens
    implementation("io.coil-kt:coil-compose:2.6.0")


    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.mlkit:face-detection:16.1.6")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Mapbox Maps SDK
    implementation("com.mapbox.maps:android-ndk27:11.17.0")

    // RTMP streaming (camera do celular)
    implementation("com.github.pedroSG94.RootEncoder:library:2.6.7")

    // Localização (GPS do operador)
    implementation("com.google.android.gms:play-services-location:21.3.0")

}


sentry {
    org.set("ifma-y3")
    projectName.set("android")

    // Evita expor codigo-fonte no Sentry; stack traces e mappings continuam sendo tratados pelo plugin.
    includeSourceContext.set(false)
}
