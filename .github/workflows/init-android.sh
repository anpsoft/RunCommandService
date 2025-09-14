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

# Создаём структуру
mkdir -p app/src/main/java/com/example/yourapp
mkdir -p gradle/wrapper

# Копируем твои файлы
cp ../AndroidManifest.xml app/src/main/
cp ../MainActivity.kt app/src/main/java/com/example/yourapp/

# Скачиваем Gradle Wrapper
curl -o gradlew https://raw.githubusercontent.com/gradle/gradle/master/gradlew
curl -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat
curl -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
curl -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties

chmod +x gradlew

# Генерируем build.gradle с твоими настройками
cat > app/build.gradle << 'EOF'
plugins {
    id 'com.android.application' version '8.4.0'
    id 'org.jetbrains.kotlin.android' version '1.9.22'
}

android {
    namespace 'com.example.yourapp'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.yourapp"
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

# Подставляем реальные значения из app.ini
sed -i "s|namespace 'com.example.yourapp'|namespace '$PACKAGE'|g" app/build.gradle
sed -i "s|compileSdk 34|compileSdk $COMPILE_SDK|g" app/build.gradle
sed -i "s|applicationId \"com.example.yourapp\"|applicationId \"$PACKAGE\"|g" app/build.gradle
sed -i "s|minSdk 21|minSdk $MIN_SDK|g" app/build.gradle
sed -i "s|targetSdk 34|targetSdk $TARGET_SDK|g" app/build.gradle
sed -i "s|versionCode 1|versionCode $VERSION_CODE|g" app/build.gradle
sed -i "s|versionName \"1.0\"|versionName \"$VERSION_NAME\"|g" app/build.gradle

# Генерируем settings.gradle — без имён проекта
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
echo "   Version: $VERSION_NAME ($VERSION_CODE)"
echo "   SDK: $MIN_SDK → $TARGET_SDK / $COMPILE_SDK"