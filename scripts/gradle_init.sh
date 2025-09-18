#!/bin/bash

# Вызывается из init-android.sh с параметрами $APP_NAME, $PACKAGE и т.д.

cat > app/build.gradle << EOF
plugins {
    id 'com.android.application' version '8.4.0'
    id 'org.jetbrains.kotlin.android' version '1.9.22'
}

android {
    namespace '$PACKAGE'
    compileSdk $COMPILE_SDK

    defaultConfig {
        applicationId '$PACKAGE'
        minSdk $MIN_SDK
        targetSdk $TARGET_SDK
        versionCode $VERSION_CODE
        versionName "$VERSION_NAME"
    }

    applicationVariants.all { variant ->
        variant.outputs.all {
            outputFileName = "$APP_NAME.apk"
        }
    }

    signingConfigs {
        debug {
            storeFile file("debug.keystore")
            storePassword "android"
            keyAlias "androiddebugkey"  
            keyPassword "android"
        }
    }

    buildTypes {
        debug {
            signingConfig signingConfigs.debug
        }
        release {
            signingConfig signingConfigs.debug
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = '1.8'
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
}
EOF

echo "✅ build.gradle сгенерирован"

echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8" > gradle.properties
echo "android.useAndroidX=true" >> gradle.properties
echo "android.enableJetifier=true" >> gradle.properties

cat > settings.gradle << EOF
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "$APP_NAME"
include ':app'
EOF

curl -fsSL -o gradlew https://raw.githubusercontent.com/gradle/gradle/master/gradlew
curl -fsSL -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat
mkdir -p gradle/wrapper
curl -fsSL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
curl -fsSL -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties

chmod +x gradlew