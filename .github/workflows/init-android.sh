#!/bin/bash

echo "=== 🚀 НАЧАЛО СБОРКИ ==="
echo "Текущая директория: $(pwd)"
ls -la .

# SDK настройки по умолчанию
DEFAULT_COMPILE_SDK=34
DEFAULT_TARGET_SDK=34
DEFAULT_MIN_SDK=21

# ----------------------------
# 1. ЧТЕНИЕ app.ini — С СЕКЦИЯМИ
# ----------------------------
APP_INI_PATH="app.ini"
if [ ! -f "$APP_INI_PATH" ]; then
  echo "❌ ОШИБКА: Файл app.ini не найден в текущей директории"
  APP_INI_PATH="../RunCommandService/app.ini"
  if [ ! -f "$APP_INI_PATH" ]; then
    echo "❌ ОШИБКА: app.ini не найден и в ../RunCommandService/"
    exit 1
  fi
fi
echo "✅ Найден app.ini: $APP_INI_PATH"

# Парсинг ini файла с секциями
current_section=""
declare -A config

while IFS= read -r line; do
    # Убираем пробелы
    line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    
    # Пропускаем комментарии и пустые строки
    [[ $line =~ ^#.*$ ]] && continue
    [[ -z $line ]] && continue
    
    # Проверяем секции
    if [[ $line =~ ^\[(.+)\]$ ]]; then
        current_section="${BASH_REMATCH[1]}"
        continue
    fi
    
    # Читаем ключ=значение
    if [[ $line =~ ^([^=]+)=(.*)$ ]]; then
        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        config["${current_section}_${key}"]="$value"
    fi
done < "$APP_INI_PATH"

# Получаем значения из секций
COMPILE_SDK=${config["SDK_compileSdk"]:-$DEFAULT_COMPILE_SDK}
TARGET_SDK=${config["SDK_targetSdk"]:-$DEFAULT_TARGET_SDK}
MIN_SDK=${config["SDK_minSdk"]:-$DEFAULT_MIN_SDK}

PACKAGE=${config["Common_package"]:-com.yourcompany.yourapp}
VERSION_CODE=${config["Common_versionCode"]:-1}
VERSION_NAME=${config["Common_versionName"]:-1.0}
APP_NAME=${config["Common_appName"]:-YourApp}
MAIN_ACTIVITY_PATH=${config["Common_mainActivityPath"]:-MainActivity.kt}
ICON_PATH=${config["Common_iconPath"]:-icon.png}
ICON_DEFAULT=${config["Common_iconDefault"]:-Terminal.png}

BUILD_TYPE=${config["Common_buildType"]:-debug}


MAIN_ENABLED=${config["MainActivity_enabled"]:-true}
MAIN_THEME=${config["MainActivity_theme"]:-AppTheme}

SHORTCUT_ENABLED=${config["ShortcutActivity_enabled"]:-true}
SHORTCUT_THEME=${config["ShortcutActivity_theme"]:-Translucent}
SHORTCUT_TOASTS=${config["ShortcutActivity_showToasts"]:-true}

SILENT_ENABLED=${config["SilentActivity_enabled"]:-false}
SILENT_THEME=${config["SilentActivity_theme"]:-NoDisplay}

echo "✅ SDK: compile=$COMPILE_SDK, target=$TARGET_SDK, min=$MIN_SDK"
echo "✅ MainActivity: enabled=$MAIN_ENABLED, theme=$MAIN_THEME"
echo "✅ ShortcutActivity: enabled=$SHORTCUT_ENABLED, theme=$SHORTCUT_THEME"
echo "✅ SilentActivity: enabled=$SILENT_ENABLED, theme=$SILENT_THEME"

# ----------------------------
# 2. ПРОВЕРКА ВСЕХ ФАЙЛОВ
# ----------------------------
for file in "$ICON_PATH" "$ICON_DEFAULT"; do
    if [ ! -f "$file" ]; then
        echo "❌ ОШИБКА: Файл не найден: $file"
        ls -la .
        exit 1
    else
        echo "✅ Найден: $file"
    fi
done

# Проверяем котлин файлы
for kotlin_file in *.kt; do
    if [ -f "$kotlin_file" ]; then
        echo "✅ Найден: $kotlin_file"
    fi
done

JAVA_PATH=$(echo "$PACKAGE" | tr '.' '/')

mkdir -p app/src/main/java/$JAVA_PATH
mkdir -p gradle/wrapper
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# ----------------------------
# 3. КОПИРОВАНИЕ И ИСПРАВЛЕНИЕ ФАЙЛОВ
# ----------------------------
# Копируем все Kotlin файлы и исправляем package
for kotlin_file in *.kt; do
    if [ -f "$kotlin_file" ]; then
        cp "$kotlin_file" app/src/main/java/$JAVA_PATH/ || { echo "❌ Не удалось скопировать $kotlin_file"; exit 1; }
        
        # Заменяем package в скопированном файле
        sed -i "s/^package .*/package $PACKAGE/" "app/src/main/java/$JAVA_PATH/$kotlin_file"
        
        echo "✅ Скопирован и исправлен: $kotlin_file"
    fi
done

# Копируем основную иконку приложения
cp "$ICON_PATH" app/src/main/res/mipmap-mdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-hdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xhdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xxhdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

# Копируем иконку для ярлыков
cp "$ICON_DEFAULT" app/src/main/res/mipmap-mdpi/ic_shortcut.png
cp "$ICON_DEFAULT" app/src/main/res/mipmap-hdpi/ic_shortcut.png
cp "$ICON_DEFAULT" app/src/main/res/mipmap-xhdpi/ic_shortcut.png
cp "$ICON_DEFAULT" app/src/main/res/mipmap-xxhdpi/ic_shortcut.png
cp "$ICON_DEFAULT" app/src/main/res/mipmap-xxxhdpi/ic_shortcut.png

# ----------------------------
# 4. ГЕНЕРАЦИЯ МАНИФЕСТА
# ----------------------------
cat > app/src/main/AndroidManifest.xml << EOF
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"/>
    <uses-permission android:name="com.termux.permission.RUN_COMMAND"/>
    
    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/$MAIN_THEME">
EOF

# Добавляем MainActivity если включена
if [ "$MAIN_ENABLED" = "true" ]; then
cat >> app/src/main/AndroidManifest.xml << EOF
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
EOF
fi

# Добавляем ShortcutActivity если включена
if [ "$SHORTCUT_ENABLED" = "true" ]; then
cat >> app/src/main/AndroidManifest.xml << EOF
        <activity
            android:name=".ShortcutActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Translucent.NoTitleBar"
            android:excludeFromRecents="true"/>
EOF
fi

# Добавляем SilentActivity если включена  
if [ "$SILENT_ENABLED" = "true" ]; then
cat >> app/src/main/AndroidManifest.xml << EOF
        <activity
            android:name=".SilentActivity"
            android:exported="false"
            android:theme="@android:style/Theme.NoDisplay"
            android:excludeFromRecents="true"/>
EOF
fi

cat >> app/src/main/AndroidManifest.xml << EOF
    </application>
</manifest>
EOF

echo "✅ Сгенерирован AndroidManifest.xml"

# ----------------------------
# 5. ГЕНЕРАЦИЯ РЕСУРСОВ (strings.xml, styles.xml, colors.xml)
# ----------------------------
cat > app/src/main/res/values/strings.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$APP_NAME</string>
</resources>
EOF

cat > app/src/main/res/values/styles.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="$MAIN_THEME" parent="android:Theme.Light">
        <!-- Простая светлая тема без AppCompat -->
    </style>
</resources>
EOF

cat > app/src/main/res/values/colors.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="colorPrimary">#008577</color>
    <color name="colorPrimaryDark">#00574B</color>
    <color name="colorAccent">#D81B60</color>
</resources>
EOF

# ----------------------------
# 6. build.gradle — МИНИМАЛЬНАЯ, РАБОЧАЯ ВЕРСИЯ ДЛЯ CI
# ----------------------------
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
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // Никаких зависимостей - только стандартный Android SDK
}
EOF

sed -i "s|___NAMESPACE___|$PACKAGE|g" app/build.gradle
sed -i "s|___COMPILE_SDK___|$COMPILE_SDK|g" app/build.gradle
sed -i "s|___PACKAGE___|$PACKAGE|g" app/build.gradle
sed -i "s|___MIN_SDK___|$MIN_SDK|g" app/build.gradle
sed -i "s|___TARGET_SDK___|$TARGET_SDK|g" app/build.gradle
sed -i "s|___VERSION_CODE___|$VERSION_CODE|g" app/build.gradle
sed -i "s|___VERSION_NAME___|$VERSION_NAME|g" app/build.gradle

# ----------------------------
# 7. gradle.properties — УВЕЛИЧИВАЕМ ПАМЯТЬ
# ----------------------------
echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8" > gradle.properties
echo "android.useAndroidX=true" >> gradle.properties
echo "android.enableJetifier=true" >> gradle.properties


# 8. settings.gradle
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

#sed -i "s|___APP_NAME___|$APP_NAME|g" settings.gradle


# ----------------------------
# 9. Gradle Wrapper
# ----------------------------
curl -fsSL -o gradlew https://raw.githubusercontent.com/gradle/gradle/master/gradlew
curl -fsSL -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat
mkdir -p gradle/wrapper
curl -fsSL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
curl -fsSL -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties

chmod +x gradlew

# ----------------------------
# 10. ПРОВЕРКА СТРУКТУРЫ (ДЛЯ ОТЛАДКИ)
# ----------------------------
echo ""
echo "=== 🔍 ПРОВЕРКА СТРУКТУРЫ ПОСЛЕ СБОРКИ ==="
find . -type f | sort | sed 's/[^\/]*\//|--- /g' | sed 's/|--- \([^|]*\)/|--- \1/g'

echo ""
echo "=== ✅ ПРОВЕРКА КРИТИЧНЫХ ФАЙЛОВ ==="
if [ -f "app/src/main/res/values/strings.xml" ]; then echo "✅ strings.xml: есть"; else echo "❌ strings.xml: отсутствует"; fi
if [ -f "app/src/main/res/mipmap-mdpi/ic_launcher.png" ]; then echo "✅ ic_launcher.png: есть во всех mipmap-папках"; else echo "❌ ic_launcher.png: отсутствует"; fi
if [ -f "app/src/main/java/$JAVA_PATH/MainActivity.kt" ]; then echo "✅ MainActivity.kt: есть"; else echo "❌ MainActivity.kt: отсутствует"; fi
if [ -f "app/build.gradle" ]; then echo "✅ build.gradle: сгенерирован"; else echo "❌ build.gradle: отсутствует"; fi
if [ -f "app/src/main/AndroidManifest.xml" ]; then echo "✅ AndroidManifest.xml: сгенерирован"; else echo "❌ AndroidManifest.xml: отсутствует"; fi

# ----------------------------
# 11. ЗАПУСК СБОРКИ
# ----------------------------
echo ""
echo "🚀 Запуск ./gradlew assemble${BUILD_TYPE^}..."
./gradlew assemble${BUILD_TYPE^}

APK_PATH="app/build/outputs/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk"
FINAL_APK="${APP_NAME}.apk"

if [ -f "$APK_PATH" ]; then
    mv "$APK_PATH" "$FINAL_APK"
    echo "✅ Итоговый APK: $FINAL_APK"
else
    echo "❌ APK не найден: $APK_PATH"
    exit 1
fi
