plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yunseong.phonemonitor"
    compileSdk = 37

    defaultConfig {
        // 스토어에 처음 올린 뒤에는 바꿀 수 없다
        applicationId = "com.yunseong.phonemonitor"
        // Android 10 미만 기기에는 Play 스토어가 설치 버튼을 보여 주지 않는다
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // R8: 안 쓰는 코드·리소스 제거 + 이름 난독화. 규칙은 proguard-rules.pro
            // 출시할 때마다 build/outputs/mapping/release/mapping.txt 를 보관한다 (크래시 기록 해석용, AAB로 올리면 Play가 함께 받음)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
