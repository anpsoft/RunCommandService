#!/bin/bash

# Читаем настройки из app.ini
source <(grep -v '^#' ../app.ini | sed 's/=/:/')

# Извлекаем значения
PACKAGE=${package:-com.example.unknown}
VERSION_CODE=${versionCode:-1}
VERSION_NAME=${versionName:-1.0}
MIN_SDK=${minSdk:-21}
TARGET_SDK=${targetSdk:-34}
COMPILE_SDK=${compileSdk:-34}

# Преобразуем package=com.anpsoft.runcommandservice → path=com/anpsoft/runcommandservice
JAVA_PATH=$(echo "$PACKAGE" | tr '.' '/')

# Создаём структуру с динамическим путём
mkdir -p app/src/main/java/$JAVA_PATH
mkdir -p gradle/wrapper

# ✅ ИСПРАВЛЕНИЕ: создаём папку app/src/main, потом копируем туда манифест
mkdir -p app/src/main
cp ../AndroidManifest.xml app/src/main/
cp ../MainActivity.kt app/src/main/java/$JAVA_PATH/

# Скачиваем Gradle Wrapper
curl -o gradlew https://raw.githubusercontent.com/gradle/gradle/master/gradlew
curl -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat
curl -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
curl -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties

chmod +x gradlew

# Генерируем build.gradle — используя PACKAGE из app.ini
cat > app/build.gradle << 'EOF'
plugins {
    id 'com.android.application' version '8.4.0'
    id 'org.jetbrains.kotlin.android' version '1.9.22'
}

android {
    namespace '___NAMESPACE___'
    compileSdk ___COMPILE_SDK___

    defaultConfig {
        applicationId '___PACKAGE___'
        minSdk ___MIN_SDK___
        targetSdk ___TARGET_SDK___
        versionCode ___VERSION_CODE___
        versionName "___VERSION_NAME___"
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

    repositories {
        google()
        mavenCentral()
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
}
EOF

# Подставляем реальные значения
sed -i "s|___NAMESPACE___|$PACKAGE|g" app/build.gradle
sed -i "s|___COMPILE_SDK___|$COMPILE_SDK|g" app/build.gradle
sed -i "s|___PACKAGE___|$PACKAGE|g" app/build.gradle
sed -i "s|___MIN_SDK___|$MIN_SDK|g" app/build.gradle
sed -i "s|___TARGET_SDK___|$TARGET_SDK|g" app/build.gradle
sed -i "s|___VERSION_CODE___|$VERSION_CODE|g" app/build.gradle
sed -i "s|___VERSION_NAME___|$VERSION_NAME|g" app/build.gradle

# Генерируем settings.gradle
cat > settings.gradle << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "UnnamedAndroidProject"
include ':app'
EOF

# Генерируем gradle.properties
echo "android.useAndroidX=true" > gradle.properties

# Готово
echo "✅ Сборка инициализирована из app.ini"
echo "   Package: $PACKAGE"
echo "   Path:    java/$JAVA_PATH"
echo "   Version: $VERSION_NAME ($VERSION_CODE)"
echo "   SDK:     $MIN_SDK → $TARGET_SDK / $COMPILE_SDK"