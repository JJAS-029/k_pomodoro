import java.util.Properties

// App del reloj (Wear OS). Mismo paquete que la del teléfono: Play las publica en la misma ficha
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jjas.labpomodoro.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jjas.labpomodoro"
        // Wear OS 3 o más nuevo
        minSdk = 30
        targetSdk = 36
        // Distinto al del teléfono en la misma versión: 1_000_000 + el del teléfono
        versionCode = 1_000_007
        versionName = "1.4.1"
    }

    // La misma llave de subida que el teléfono (android/keystore.properties)
    val keystoreFile = rootProject.file("keystore.properties")
    val uploadKey = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use(::load) }
    signingConfigs {
        if (!uploadKey.isEmpty) {
            create("upload") {
                storeFile = rootProject.file(uploadKey.getProperty("storeFile"))
                storePassword = uploadKey.getProperty("storePassword")
                keyAlias = uploadKey.getProperty("keyAlias")
                keyPassword = uploadKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.play.services.wearable)
    implementation(libs.wear.ongoing)
    implementation(libs.wear.tiles)
    implementation(libs.wear.complications)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.guava)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
}
