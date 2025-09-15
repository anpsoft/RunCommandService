#!/bin/bash

# Выводим текущую директорию и содержимое для отладки
echo "Текущая директория: $(pwd)"
echo "Содержимое корня репозитория:"
ls -la .
echo "Содержимое родительской директории:"
ls -la ../
echo "Содержимое ../RunCommandService (если существует):"
ls -la ../RunCommandService/ || echo "Папка ../RunCommandService/ не найдена"

# Проверяем наличие app.ini
APP_INI_PATH="app.ini"
if [ ! -f "$APP_INI_PATH" ]; then
  echo "❌ Ошибка: Файл app.ini не найден в текущей директории"
  APP_INI_PATH="../RunCommandService/app.ini"
  if [ ! -f "$APP_INI_PATH" ]; then
    echo "❌ Ошибка: Файл app.ini не найден и в ../RunCommandService/"
    exit 1
  fi
fi
echo "Найден app.ini по пути: $APP_INI_PATH"

# Читаем настройки из app.ini
source <(grep -v '^#' "$APP_INI_PATH" | sed 's/=/:/')

# Извлекаем значения
PACKAGE=${package:-com.yourcompany.yourapp}
VERSION_CODE=${versionCode:-1}
VERSION_NAME=${versionName:-1.0}
MIN_SDK=${minSdk:-21}
TARGET_SDK=${targetSdk:-34}
COMPILE_SDK=${compileSdk:-34}

# Преобразуем package=com.yourcompany.yourapp → path=com/yourcompany/yourapp
JAVA_PATH=$(echo "$PACKAGE" | tr '.' '/')

# Создаём структуру с динамическим путём
mkdir -p app/src/main/java/$JAVA_PATH
mkdir -p gradle/wrapper
mkdir -p app/src/main

# Проверяем наличие AndroidManifest.xml (с учётом возможного регистра)
MANIFEST_PATH="AndroidManifest.xml"
if [ ! -f "$MANIFEST_PATH" ]; then
  MANIFEST_PATH="androidmanifest.xml" # Проверяем вариант с нижним регистром
  if [ ! -f "$MANIFEST_PATH" ]; then
    MANIFEST_PATH="../RunCommandService/AndroidManifest.xml"
    if [ ! -f "$MANIFEST_PATH" ]; then
      echo "❌ Ошибка: Файл AndroidManifest.xml не найден ни в текущей директории, ни в ../RunCommandService/"
      exit 1
    fi
  fi
fi
echo "Найден AndroidManifest.xml по пути: $MANIFEST_PATH"

# Проверяем наличие MainActivity.kt
MAIN_ACTIVITY_PATH="MainActivity.kt"
if [ ! -f "$MAIN_ACTIVITY_PATH" ]; then
  MAIN_ACTIVITY_PATH="../RunCommandService/MainActivity.kt"
  if [ ! -f "$MAIN_ACTIVITY_PATH" ]; then
    echo "❌ Ошибка: Файл MainActivity.kt не найден ни в текущей директории, ни в ../RunCommandService/"
    exit 1
  fi
fi
echo "Найден MainActivity.kt по пути: $MAIN_ACTIVITY_PATH"

# Копируем манифест и MainActivity.kt, проверяем успешность копирования
cp "$MANIFEST_PATH" app/src/main/ || { echo "❌ Ошибка копирования AndroidManifest.xml"; exit 1; }
cp "$MAIN_ACTIVITY_PATH" app/src/main/java/$JAVA_PATH/ || { echo "❌ Ошибка копирования MainActivity.kt"; exit 1; }

# Проверяем, что манифест скопирован
if [ -f app/src/main/AndroidManifest.xml ]; then
  echo "✅ AndroidManifest.xml успешно скопирован в app/src/main/"
else
  echo "❌ Ошибка: AndroidManifest.xml не найден в app/src/main/ после копирования"
  exit 1
fi

# Скачиваем Gradle Wrapper
curl -o gradlew https://raw.githubusercontent.com/gradle/gradle/master/gradlew
curl -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat
curl -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
curl -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties

chmod +x gradlew

# Генерируем build.gradle
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

# Чтение appName и theme из app.ini
APP_NAME=${appName:-YourApp}
THEME_NAME=${theme:-AppTheme}

# Создаём папки ресурсов
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# Генерируем strings.xml
cat > app/src/main/res/values/strings.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$APP_NAME</string>
</resources>
EOF

# Генерируем styles.xml
cat > app/src/main/res/values/styles.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="$THEME_NAME" parent="Theme.AppCompat.Light.DarkActionBar">
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
        <item name="colorAccent">@color/colorAccent</item>
    </style>
</resources>
EOF

# Генерируем colors.xml (обязательно для styles.xml)
cat > app/src/main/res/values/colors.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="colorPrimary">#008577</color>
    <color name="colorPrimaryDark">#00574B</color>
    <color name="colorAccent">#D81B60</color>
</resources>
EOF

# Копируем icon.png в все mipmap-папки
cp icon.png app/src/main/res/mipmap-mdpi/ic_launcher.png
cp icon.png app/src/main/res/mipmap-hdpi/ic_launcher.png
cp icon.png app/src/main/res/mipmap-xhdpi/ic_launcher.png
cp icon.png app/src/main/res/mipmap-xxhdpi/ic_launcher.png
cp icon.png app/src/main/res/mipmap-xxxhdpi/ic_launcher.png