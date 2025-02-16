plugins {
    id("com.android.application")
}

android {
    namespace = "net.opengress.plantlookup"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.opengress.plantlookup"
        // 14: needed for viewbinding
        // 16: required for sqlite statements
        // 18: needed for webp icons
        minSdk = 16
        targetSdk = 35
        versionCode = 3
        versionName = "0.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    // newer versions of these have minsdk 21
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("org.apache.commons:commons-csv:1.13.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
