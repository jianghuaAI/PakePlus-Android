plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.app.pakeplus"
    compileSdk = 34

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("pakeplus.keystore")
            storePassword = "1024xiaoshen"
            keyPassword = "1024xiaoshen"
            keyAlias = "pakeplus"
        }
    }

    defaultConfig {
        applicationId = "com.oaikes.pakeplus.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            // 关闭 R8：保留源码类名/方法名，便于 logcat 定位；同时避免混淆删 FileProvider 等
            isMinifyEnabled = false
            // 关闭资源压缩：R8 会把 res/xml/provider_paths.xml 误判为无引用而删除，
            // 导致拍照时 FileProvider.getUriForFile 抛 IllegalArgumentException 闪退。
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // CameraX：App 内自绘相机（上课打卡拍照，规避 OEM 系统相机 App 闪退）
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // 拍照 JPEG 的 EXIF 旋转校正
    implementation(libs.androidx.exifinterface)
}