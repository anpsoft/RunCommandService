#!/bin/bash

# Читаем значения из app.ini
PACKAGE=$(grep "^package=" app.ini | cut -d'=' -f2)
APP_NAME=$(grep "^appName=" app.ini | cut -d'=' -f2)

# Проверяем, что значения существуют
if [ -z "$PACKAGE" ] || [ -z "$APP_NAME" ]; then
    echo "❌ Ошибка: package или appName отсутствуют в app.ini"
    exit 1
fi

echo "📊 Прочитанные значения:"
echo "PACKAGE=$PACKAGE"
echo "APP_NAME=$APP_NAME"

# Начинаем генерацию манифеста
cat << EOF > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$PACKAGE">
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"/>
    <uses-permission android:name="com.android.launcher.permission.UNINSTALL_SHORTCUT" />
    <uses-permission android:name="com.termux.permission.RUN_COMMAND"/>
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="$APP_NAME"
        android:theme="@android:style/Theme.DeviceDefault.Light">
EOF

# Получаем список активностей из app.ini
activities=$(awk '/^\[.*Activity\]/{print substr($0,2,length($0)-2)}' app.ini)

# Добавляем активности
for activity in $activities; do
    enabled=$(awk "/^\[$activity\]/{flag=1; next} /^\[/{flag=0} flag && /^enabled=/{print \$0}" app.ini | cut -d'=' -f2)
    theme=$(awk "/^\[$activity\]/{flag=1; next} /^\[/{flag=0} flag && /^theme=/{print \$0}" app.ini | cut -d'=' -f2)
    exported=$(awk "/^\[$activity\]/{flag=1; next} /^\[/{flag=0} flag && /^exported=/{print \$0}" app.ini | cut -d'=' -f2)
    intentFilter=$(awk "/^\[$activity\]/{flag=1; next} /^\[/{flag=0} flag && /^intentFilter=/{print \$0}" app.ini | cut -d'=' -f2)
    enabled=${enabled:-"false"}
    theme=${theme:-"DeviceDefault.Light"}
    exported=${exported:-"false"}
    intentFilter=${intentFilter:-"NONE"}
    echo "🔍 Проверяем $activity: enabled=$enabled, theme=$theme, exported=$exported, intentFilter=$intentFilter"
    if [ "$enabled" = "true" ]; then
        if [ "$exported" = "true" ] && [ "$intentFilter" = "SHORTCUT" ]; then
            cat << EOF >> app/src/main/AndroidManifest.xml
        <activity
            android:name=".$activity"
            android:exported="$exported"
            android:enabled="$enabled"
            android:theme="@android:style/Theme.$theme">
            <intent-filter>
                <action android:name="android.intent.action.CREATE_SHORTCUT" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
EOF
        elif [ "$exported" = "true" ] && [ "$intentFilter" = "MAIN" ]; then
            cat << EOF >> app/src/main/AndroidManifest.xml
        <activity
            android:name=".$activity"
            android:exported="$exported"
            android:enabled="$enabled"
            android:theme="@android:style/Theme.$theme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
EOF
        else
            cat << EOF >> app/src/main/AndroidManifest.xml
        <activity
            android:name=".$activity"
            android:exported="$exported"
            android:enabled="$enabled"
            android:theme="@android:style/Theme.$theme" />
EOF
        fi
        echo "✅ Добавлена активити: $activity (.$activity)"
    else
        echo "❌ Пропущена активити: $activity (enabled=$enabled)"
    fi
done

cat << EOF >> app/src/main/AndroidManifest.xml
    </application>
</manifest>
EOF

# Копирование XML из templates/layout/
if [ -d "templates/layout" ]; then
    for xml_file in $(find templates/layout -name "*.xml"); do
        xml_name=$(basename "$xml_file")
        cp "$xml_file" "app/src/main/res/layout/$xml_name"
        echo "✅ Копирован: $xml_name в layout"
    done
fi
echo "✅ Ресурсы сгенерированы"

# Выводим сгенерированный манифест в лог для проверки
echo "📄 Сгенерированный AndroidManifest.xml:"
echo "=================================="
cat app/src/main/AndroidManifest.xml
echo "=================================="
echo "✅ AndroidManifest.xml создан успешно"