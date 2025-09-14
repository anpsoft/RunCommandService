#!/bin/bash

# Создаём нужные папки
mkdir -p app/src/main/java/com/example/runcommandservice
mkdir -p app/src/main/res/values
mkdir -p gradle/wrapper

# Копируем твои файлы в правильные места
cp AndroidManifest.xml app/src/main/
cp MainActivity.kt app/src/main/java/com/example/runcommandservice/
cp list app/src/main/res/values/ || cp list app/src/main/assets/

# Скачиваем Gradle Wrapper
curl -o gradlew https://raw.githubusercontent.com/gradle/gradle/master/gradlew
curl -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat
curl -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
curl -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties
chmod +x gradlew

# Генерируем build.gradle
cat > app/build.gradle << 'EOF'
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.example.runcommandservice'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.runcommandservice"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
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

dependencies {
    implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
}
EOF

# ✅ СОЗДАЁМ settings.gradle — ВСЁ, ЧТО НУЖНО ДЛЯ GRADLE 9+
echo "include ':app'" > settings.gradle