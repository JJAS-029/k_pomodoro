plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)            // procesador de anotaciones (Hilt, Room)
    alias(libs.plugins.hilt)           // inyección de dependencias
    alias(libs.plugins.google.services) // lee app/google-services.json
}

android {
    namespace = "com.jjas.labpomodoro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jjas.labpomodoro"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AdMob: mientras no haya cuenta se usan los IDs de prueba oficiales de Google. Los reales
        // van en gradle.properties (admobAppId, admobBannerId, admobInterstitialId)
        val admobAppId = providers.gradleProperty("admobAppId").getOrElse("ca-app-pub-3940256099942544~3347511713")
        val admobBanner = providers.gradleProperty("admobBannerId").getOrElse("ca-app-pub-3940256099942544/9214589741")
        val admobInterstitial = providers.gradleProperty("admobInterstitialId").getOrElse("ca-app-pub-3940256099942544/1033173712")
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "AD_BANNER_ID", "\"$admobBanner\"")
        buildConfigField("String", "AD_INTERSTITIAL_ID", "\"$admobInterstitial\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // AGP 9 compila Kotlin de forma nativa y toma el jvmTarget de aquí
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Los esquemas exportados de Room sirven a los tests de migración
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")
}

ksp {
    // Room exporta el esquema de la BD (útil para migraciones en la Fase 1)
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose + Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Lifecycle / ViewModel / Navegación
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // La navegación trae la 1.7.3, que choca con room-testing (AbstractMethodError); se sube a la estable
    implementation(libs.kotlinx.serialization.core)

    // Fase 5: compras (Pro), anuncios y consentimiento de privacidad para anuncios
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    // Lugares (Pro): ubicación aproximada al iniciar el plan
    implementation(libs.play.services.location)

    // Corrutinas + DataStore
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.datastore.preferences)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)

    // Google Sign-In (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}