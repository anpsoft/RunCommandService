#!/bin/bash

echo "Текущая директория: $(pwd)"
echo "Содержимое корня репозитория:"
ls -la .

# Проверяем app.ini
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

# Читаем настройки
#source <(grep -v '^#' "$APP_INI_PATH" | sed 's/=/:/')

while IFS='=' read -r key value; do
    [[ $key =~ ^[[:space:]]*# ]] && continue
    [[ -z $key ]] && continue
    export "$key=$value"
done < "$APP_INI_PATH"



PACKAGE=${package:-com.yourcompany.yourapp}
VERSION_CODE=${versionCode:-1}
VERSION_NAME=${versionName:-1.0}
MIN_SDK=${minSdk:-21}
TARGET_SDK=${targetSdk:-34}
COMPILE_SDK=${compileSdk:-34}
APP_NAME=${appName:-YourApp}
THEME_NAME=${theme:-AppTheme}

JAVA_PATH=$(echo "$PACKAGE" | tr '.' '/')

mkdir -p app/src/main/java/$JAVA_PATH
mkdir -p gradle/wrapper
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# Проверяем AndroidManifest.xml
MANIFEST_PATH="AndroidManifest.xml"
if [ ! -f "$MANIFEST_PATH" ]; then
  MANIFEST_PATH="androidmanifest.xml"
  if [ ! -f "$MANIFEST_PATH" ]; then
    MANIFEST_PATH="../RunCommandService/AndroidManifest.xml"
    if [ ! -f "$MANIFEST_PATH" ]; then
      echo "❌ Ошибка: AndroidManifest.xml не найден"
      exit 1
    fi
  fi
fi
echo "Найден AndroidManifest.xml по пути: $MANIFEST_PATH"

# Копируем манифест
cp "$MANIFEST_PATH" app/src/main/ || { echo "❌ Ошибка копирования AndroidManifest.xml"; exit 1; }

# Проверяем MainActivity.kt
MAIN_ACTIVITY_PATH="MainActivity.kt"
if [ ! -f "$MAIN_ACTIVITY_PATH" ]; then
  MAIN_ACTIVITY_PATH="../RunCommandService/MainActivity.kt"
  if [ ! -f "$MAIN_ACTIVITY_PATH" ]; then
    echo "❌ Ошибка: MainActivity.kt не найден"
    exit 1
  fi
fi
echo "Найден MainActivity.kt по пути: $MAIN_ACTIVITY_PATH"

# Копируем MainActivity.kt
cp "$MAIN_ACTIVITY_PATH" app/src/main/java/$JAVA_PATH/ || { echo "❌ Ошибка копирования MainActivity.kt"; exit 1; }

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

# Генерируем colors.xml
cat > app/src/main/res/values/colors.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="colorPrimary">#008577</color>
    <color name="colorPrimaryDark">#00574B</color>
    <color name="colorAccent">#D81B60</color>
</resources>
EOF

# Копируем icon.png в mipmap-папки (из корня репозитория)
cp icon.png app/src/main/res/mipmap-mdpi/ic_launcher.png
cp icon.png app/src/main/res/mipmap-hdpi/ic_launcher.png
cp icon.png app/src/main/res/mipmap-xhdpi/ic_launcher.png
cp icon.png app/src/main/res/mipmap-xxhdpi/ic_launcher.png
cp icon.png app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

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

    // --- Android Compose ---
    implementation 'androidx.activity:activity-compose:1.9.0'
    implementation 'androidx.compose.ui:ui:1.6.7'
    implementation 'androidx.compose.ui:ui-tooling-preview:1.6.7'
    implementation 'androidx.compose.material3:material3:1.2.0'
}
EOF

# Подставляем значения
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

# Скачиваем Gradle Wrapper
curl -o gradlew https://raw.githubusercontent.com/gradle/gradle/master/gradlew
curl -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat
curl -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
curl -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties

chmod +x gradlew

echo "✅ Сборка инициализирована из app.ini"
echo "   Package: $PACKAGE"
echo "   Path:    java/$JAVA_PATH"
echo "   Version: $VERSION_NAME ($VERSION_CODE)"
echo "   SDK:     $MIN_SDK → $TARGET_SDK / $COMPILE_SDK"
echo "   App Name: $APP_NAME"
echo "   Theme:   $THEME_NAME"
echo "   Icon:    icon.png (скопирован в mipmap)"

echo "🚀 Запуск ./gradlew assembleDebug..."
./gradlew assembleDebug