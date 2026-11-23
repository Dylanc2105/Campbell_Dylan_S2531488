import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

val trafficScotlandClientId =
    localProperties.getProperty(
        "TRAFFIC_SCOTLAND_CLIENT_ID", ""
    )

val trafficScotlandClientSecret =
    localProperties.getProperty(
        "TRAFFIC_SCOTLAND_CLIENT_SECRET", ""
    )

val googleMapsApiKey =
    localProperties.getProperty(
        "GOOGLE_MAPS_API_KEY", ""
    )

fun escapeJavaString(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}

android {
    namespace = "com.example.campbell_dylan_s2531488"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId =
            "com.example.campbell_dylan_s2531488"

        minSdk = 26
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "TRAFFIC_SCOTLAND_CLIENT_ID",
            "\"${escapeJavaString(trafficScotlandClientId)}\""
        )

        buildConfigField(
            "String",
            "TRAFFIC_SCOTLAND_CLIENT_SECRET",
            "\"${escapeJavaString(trafficScotlandClientSecret)}\""
        )

        manifestPlaceholders["googleMapsApiKey"] =
            googleMapsApiKey
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)

    implementation(
        "com.google.android.gms:play-services-maps:20.0.0"
    )

    testImplementation(libs.junit)

    androidTestImplementation(
        libs.espresso.core
    )

    androidTestImplementation(
        libs.ext.junit
    )
}
