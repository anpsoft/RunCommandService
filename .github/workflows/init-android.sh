#!/bin/bash

echo "=== 🚀 НАЧАЛО СБОРКИ ==="
echo "Текущая директория: $(pwd)"
ls -la .

# ----------------------------
# 1. ЧТЕНИЕ app.ini — БЕЗ ОШИБОК
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

while IFS='=' read -r key value; do
    [[ $key =~ ^[[:space:]]*# ]] && continue
    [[ -z $key ]] && continue
    export "$key=$value"
done < "$APP_INI_PATH"

# ----------------------------
# 1a. Обработка нового параметра
# ----------------------------
NEW_MANIFEST_PARAM=${newManifestParam:-default_value}  # значение по умолчанию
echo "✅ Параметр newManifestParam: $NEW_MANIFEST_PARAM"

PACKAGE=${package:-com.yourcompany.yourapp}
VERSION_CODE=${versionCode:-1}
VERSION_NAME=${versionName:-1.0}
MIN_SDK=${minSdk:-21}
TARGET_SDK=${targetSdk:-34}
COMPILE_SDK=${compileSdk:-34}
APP_NAME=${appName:-YourApp}
THEME_NAME=${theme:-AppTheme}

MANIFEST_PATH=${manifestPath:-AndroidManifest.xml}
MAIN_ACTIVITY_PATH=${mainActivityPath:-MainActivity.kt}
ICON_PATH=${iconPath:-icon.png}

# ----------------------------
# 2. ПРОВЕРКА ВСЕХ ФАЙЛОВ
# ----------------------------
for file in "$MANIFEST_PATH" "$MAIN_ACTIVITY_PATH" "$ICON_PATH"; do
    if [ ! -f "$file" ]; then
        echo "❌ ОШИБКА: Файл не найден: $file"
        ls -la .
        exit 1
    else
        echo "✅ Найден: $file"
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
# 3. КОПИРОВАНИЕ ФАЙЛОВ
# ----------------------------
cp "$MANIFEST_PATH" app/src/main/ || { echo "❌ Не удалось скопировать манифест"; exit 1; }
cp "$MAIN_ACTIVITY_PATH" app/src/main/java/$JAVA_PATH/ || { echo "❌ Не удалось скопировать MainActivity.kt"; exit 1; }

cp "$ICON_PATH" app/src/main/res/mipmap-mdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-hdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xhdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xxhdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

# ----------------------------
# 4. ГЕНЕРАЦИЯ РЕСУРСОВ (strings.xml, styles.xml, colors.xml)
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
    <style name="$THEME_NAME" parent="Theme.AppCompat.Light.DarkActionBar">
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
        <item name="colorAccent">@color/colorAccent</item>
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
# 5. build.gradle — МИНИМАЛЬНАЯ, РАБОЧАЯ ВЕРСИЯ ДЛЯ CI
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
# 6. gradle.properties — УВЕЛИЧИВАЕМ ПАМЯТЬ
# ----------------------------
echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8" > gradle.properties
echo "android.useAndroidX=true" >> gradle.properties
echo "android.enableJetifier=true" >> gradle.properties

# ----------------------------
# 7. settings.gradle
# ----------------------------
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

# ----------------------------
# 8. Gradle Wrapper
# ----------------------------
curl -fsSL -o gradlew https://raw.githubusercontent.com/gradle/gradle/master/gradlew
curl -fsSL -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat
mkdir -p gradle/wrapper
curl -fsSL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
curl -fsSL -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties

chmod +x gradlew

# ----------------------------
# 9. ПРОВЕРКА СТРУКТУРЫ (ДЛЯ ОТЛАДКИ)
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

# ----------------------------
# 10. ЗАПУСК СБОРКИ
# ----------------------------
echo ""
echo "🚀 Запуск ./gradlew assembleDebug..."
./gradlew assembleDebug
