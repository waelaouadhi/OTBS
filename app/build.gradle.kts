plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.onetechbs"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.onetechbs"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

}

dependencies {

    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose libraries
    implementation(platform(libs.androidx.compose.bom)) // Ensure only this is used
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Support libraries
    implementation(libs.androidx.appcompat) // For AppCompatActivity support
    implementation(libs.material) // Material Design components
    implementation(libs.androidx.constraintlayout) // For ConstraintLayout
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Additional libraries
    implementation("com.github.sundeepk:compact-calendar-view:3.0.0") // Ensure latest version

    // Fragment and Navigation
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    implementation(libs.firebase.appdistribution.gradle)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.androidx.swiperefreshlayout)
    implementation("com.google.code.gson:gson:2.13.1")
    implementation(libs.androidx.tools.core)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)

    // Optional: If you need additional versions for material or core libraries, verify and update.
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // or a recent version
    implementation("com.google.android.material:material:1.13.0-alpha11")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("io.github.architshah248.calendar:awesome-calendar:2.0.0")
    implementation("com.applandeo:material-calendar-view:1.9.2")

    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.google.android.material:material:1.0.0")


    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")

    implementation ("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation ("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation ("com.ramotion.foldingcell:folding-cell:1.2.3")

    // Lottie Animation
    implementation ("com.airbnb.android:lottie:6.4.0")
    implementation ("com.kizitonwose.calendar:view:2.4.0")




}   