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
    line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    [[ $line =~ ^#.*$ ]] && continue
    [[ -z $line ]] && continue
    if [[ $line =~ ^\[(.+)\]$ ]]; then
        current_section="${BASH_REMATCH[1]}"
        continue
    fi
    if [[ $line =~ ^([^=]+)=(.*)$ ]]; then
        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        config["${current_section}_${key}"]="$value"
    fi
done < "$APP_INI_PATH"

COMPILE_SDK=${config["SDK_compileSdk"]:-$DEFAULT_COMPILE_SDK}
TARGET_SDK=${config["SDK_targetSdk"]:-$DEFAULT_TARGET_SDK}
MIN_SDK=${config["SDK_minSdk"]:-$DEFAULT_MIN_SDK}

APP_NAME=${config["Common_appName"]:-YourApp}
PACKAGE_BASE=${config["Common_packageBase"]:-com.yourcompany.yourapp}
PACKAGE="$PACKAGE_BASE$(echo $APP_NAME | sed 's/YourApp//')" # Уникальный package, напр. com.yourcompany.yourapp3
VERSION_CODE=${config["Common_versionCode"]:-1}
VERSION_NAME=${config["Common_versionName"]:-1.0}
MAIN_ACTIVITY_PATH=${config["Common_mainActivityPath"]:-MainActivity.kt}
ICON_PATH=${config["Common_iconPath"]:-icon.png}
ICON_DEFAULT=${config["Common_iconDefault"]:-Terminal.png}
ICON_NO_ICON=${config["Common_iconNoIcon"]:-no_icon.png}
BUILD_TYPE=${config["Common_buildType"]:-debug}

MAIN_ENABLED=${config["MainActivity_enabled"]:-true}
MAIN_THEME=${config["MainActivity_theme"]:-AppTheme}

SHORTCUT_ENABLED=${config["ShortcutActivity_enabled"]:-true}
SHORTCUT_THEME=${config["ShortcutActivity_theme"]:-Translucent}
SHORTCUT_TOASTS=${config["ShortcutActivity_showToasts"]:-true}

SILENT_ENABLED=${config["SilentActivity_enabled"]:-false}
SILENT_THEME=${config["SilentActivity_theme"]:-NoDisplay}

echo "✅ SDK: compile=$COMPILE_SDK, target=$TARGET_SDK, min=$MIN_SDK"
echo "✅ Package: $PACKAGE"
echo "✅ MainActivity: enabled=$MAIN_ENABLED, theme=$MAIN_THEME"
echo "✅ ShortcutActivity: enabled=$SHORTCUT_ENABLED, theme=$SHORTCUT_THEME"
echo "✅ SilentActivity: enabled=$SILENT_ENABLED, theme=$SILENT_THEME"

# ----------------------------
# 2. ПРОВЕРКА ВСЕХ ФАЙЛОВ
# ----------------------------
for file in "$ICON_PATH" "$ICON_DEFAULT" "$ICON_NO_ICON"; do
    if [ ! -f "$file" ]; then
        echo "❌ ОШИБКА: Файл не найден: $file"
        ls -la .
        exit 1
    else
        echo "✅ Найден: $file"
    fi
done

for kotlin_file in *.kt; do
    if [ -f "$kotlin_file" ]; then
        echo "✅ Найден: $kotlin_file"
    fi
done

JAVA_PATH=$(echo "$PACKAGE" | tr '.' '/')

mkdir -p app/src/main/java/$JAVA_PATH
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# Создаем debug keystore
echo "✅ Создание debug keystore..."
keytool -genkeypair -v -keystore app/debug.keystore -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=Debug,OU=Debug,O=Debug,L=Debug,ST=Debug,C=US" 2>/dev/null

if [ -f "app/debug.keystore" ]; then
    echo "✅ Keystore создан"
else
    echo "❌ Ошибка создания keystore"
fi

# ----------------------------
# 3. КОПИРОВАНИЕ И ИСПРАВЛЕНИЕ ФАЙЛОВ
# ----------------------------
for kotlin_file in *.kt; do
    if [ -f "$kotlin_file" ]; then
        cp "$kotlin_file" app/src/main/java/$JAVA_PATH/ || { echo "❌ Не удалось скопировать $kotlin_file"; exit 1; }
        sed -i "s/^package .*/package $PACKAGE/" "app/src/main/java/$JAVA_PATH/$kotlin_file"
        echo "✅ Скопирован и исправлен: $kotlin_file"
    fi
done

cp "$ICON_PATH" app/src/main/res/mipmap-mdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-hdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xhdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xxhdpi/ic_launcher.png
cp "$ICON_PATH" app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

cp "$ICON_DEFAULT" app/src/main/res/mipmap-mdpi/ic_shortcut.png
cp "$ICON_DEFAULT" app/src/main/res/mipmap-hdpi/ic_shortcut.png
cp "$ICON_DEFAULT" app/src/main/res/mipmap-xhdpi/ic_shortcut.png
cp "$ICON_DEFAULT" app/src/main/res/mipmap-xxhdpi/ic_shortcut.png
cp "$ICON_DEFAULT" app/src/main/res/mipmap-xxxhdpi/ic_shortcut.png

cp "$ICON_NO_ICON" app/src/main/res/mipmap-mdpi/ic_no_icon.png
cp "$ICON_NO_ICON" app/src/main/res/mipmap-hdpi/ic_no_icon.png
cp "$ICON_NO_ICON" app/src/main/res/mipmap-xhdpi/ic_no_icon.png
cp "$ICON_NO_ICON" app/src/main/res/mipmap-xxhdpi/ic_no_icon.png
cp "$ICON_NO_ICON" app/src/main/res/mipmap-xxxhdpi/ic_no_icon.png

# ----------------------------
# 4. ГЕНЕРАЦИЯ МАНИФЕСТА
# ----------------------------
cat > app/src/main/AndroidManifest.xml << EOF
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"/>
    <uses-permission android:name="com.termux.permission.RUN_COMMAND"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    
    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/$MAIN_THEME">
EOF

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

if [ "$SHORTCUT_ENABLED" = "true" ]; then
cat >> app/src/main/AndroidManifest.xml << EOF
        <activity
            android:name=".ShortcutActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Translucent.NoTitleBar"
            android:excludeFromRecents="true"/>
EOF
fi

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
        <activity
            android:name=".ScriptSettingsActivity"
            android:exported="false"
            android:theme="@style/$MAIN_THEME"/>
    </application>
</manifest>
EOF

echo "✅ Сгенерирован AndroidManifest.xml"

# ----------------------------
# 5. ГЕНЕРАЦИЯ РЕСУРСОВ
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

cat > app/src/main/res/layout/script_item.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="8dp">
    <ImageView
        android:id="@+id/script_icon"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:src="@mipmap/ic_no_icon"/>
    <LinearLayout
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        <TextView
            android:id="@+id/script_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="16sp"/>
        <TextView
            android:id="@+id/script_description"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="12sp"/>
    </LinearLayout>
    <CheckBox
        android:id="@+id/active_checkbox"
        android:layout_width="48dp"
        android:layout_height="wrap_content"
        android:text=""/>
    <CheckBox
        android:id="@+id/shortcut_checkbox"
        android:layout_width="48dp"
        android:layout_height="wrap_content"
        android:text=""/>
    <Button
        android:id="@+id/test_button"
        android:layout_width="60dp"
        android:layout_height="wrap_content"
        android:text="Тест"/>
</LinearLayout>
EOF

cat > app/src/main/res/layout/activity_script_settings.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    <EditText
        android:id="@+id/name_edit"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Имя скрипта"/>
    <EditText
        android:id="@+id/description_edit"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Описание"/>
    <ImageView
        android:id="@+id/icon_view"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:src="@mipmap/ic_no_icon"/>
    <Button
        android:id="@+id/icon_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Выбрать иконку"/>
    <CheckBox
        android:id="@+id/active_checkbox"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Активен"/>
    <Button
        android:id="@+id/rename_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Переименовать"/>
    <Button
        android:id="@+id/delete_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Удалить"/>
</LinearLayout>
EOF

# ----------------------------
# 6. ВЫЗОВ gradle_init.sh
# ----------------------------
if [ -f "$(dirname "$0")/gradle_init.sh" ]; then
    chmod +x "$(dirname "$0")/gradle_init.sh"
    . "$(dirname "$0")/gradle_init.sh"
else
    echo "❌ ОШИБКА: gradle_init.sh не найден в $(dirname "$0")"
    exit 1
fi

# ----------------------------
# 7. ПРОВЕРКА СТРУКТУРЫ
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
# 8. ЗАПУСК СБОРКИ
# ----------------------------
echo ""
echo "🚀 Запуск ./gradlew assemble${BUILD_TYPE^}..."
./gradlew assemble${BUILD_TYPE^}

APK_PATH="app/build/outputs/apk/$BUILD_TYPE/$APP_NAME.apk"

if [ -f "$APK_PATH" ]; then
    echo "✅ Итоговый APK: $APK_PATH"
else
    echo "❌ APK не найден: $APK_PATH"
    exit 1
fi