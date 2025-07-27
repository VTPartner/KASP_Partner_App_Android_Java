plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.kapstranspvtltd.kaps_partner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kapstranspvtltd.kaps_partner"
        minSdk = 27
        targetSdk = 34
        versionCode = 10
        versionName = "0.0.1"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation ("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation ("com.google.android.libraries.places:places:4.1.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation ("com.google.code.gson:gson:2.11.0")

    implementation ("com.tbuonomo:dotsindicator:4.3")
    implementation ("com.squareup.retrofit2:retrofit:2.11.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation ("com.onesignal:OneSignal:[4.0.0, 4.99.99]")
    implementation ("com.google.android.gms:play-services-places:17.1.0")
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation ("com.google.android.gms:play-services-maps:19.1.0")
//    implementation 'com.google.firebase:firebase-auth:19.3.2'
//    implementation 'com.google.firebase:firebase-messaging:20.2.4'
//    implementation 'com.google.firebase:firebase-database:19.3.1'
    implementation ("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    implementation ("com.razorpay:checkout:1.6.41")
    implementation ("com.paypal.sdk:paypal-android-sdk:2.16.0")
    implementation ("com.stripe:stripe-android:14.5.0")
    implementation ("com.stripe:stripe-java:19.23.0")

    implementation ("co.paystack.android:paystack:3.3.2")
    implementation ("co.paystack.android.design.widget:pinpad:1.0.8")

    //PolyUtil
    implementation ("com.google.maps.android:android-maps-utils:3.4.0")
    //
    implementation ("org.slf4j:slf4j-simple:1.7.25")
    implementation ("com.google.maps:google-maps-services:2.1.2")
    //Camera
    implementation ("androidx.camera:camera-core:1.3.1")
    implementation ("androidx.camera:camera-camera2:1.3.1")
    implementation ("androidx.camera:camera-lifecycle:1.3.1")
    implementation ("androidx.camera:camera-view:1.3.1")
    implementation ("androidx.camera:camera-extensions:1.3.1")

    // CircleImageView
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    //Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    //Firebase Messaging
    implementation("com.google.firebase:firebase-messaging:24.1.1")
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")

    //Chip
    implementation ("org.threeten:threetenbp:1.5.1")

    //Lottie animation
    implementation ("com.airbnb.android:lottie:3.4.0")
}