#!/bin/bash

# Читаем значения из ini
PACKAGE=$(grep "^package=" app.ini| cut -d'=' -f2)
APP_NAME=$(grep "^appName=" app.ini| cut -d'=' -f2)

# Проверяем, что значения существуют
if [ -z "$PACKAGE" ] || [ -z "$APP_NAME" ]; then
    echo "❌ Ошибка: package или appName отсутствуют в build.ini"
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

# Добавляем активности из ini
for activity in PermissionActivity MainActivity ScriptSettingsActivity AboutActivity InstructionsActivity SettingsActivity ShortcutActivity SilentActivity; do
    enabled=$(awk "/^\[$activity\]/{flag=1; next} /^\[/{flag=0} flag && /^enabled=/{print \$0}" app.ini| cut -d'=' -f2)
    package=$(awk "/^\[$activity\]/{flag=1; next} /^\[/{flag=0} flag && /^package=/{print \$0}" app.ini| cut -d'=' -f2)
    theme=$(awk "/^\[$activity\]/{flag=1; next} /^\[/{flag=0} flag && /^theme=/{print \$0}" app.ini| cut -d'=' -f2)
    enabled=${enabled:-"false"}
    package=${package:-"$PACKAGE"}
    theme=${theme:-"DeviceDefault.Light"}
    echo "🔍 Проверяем $activity: enabled=$enabled, package=$package, theme=$theme"
    if [ "$enabled" = "true" ]; then
        if [ "$activity" = "ShortcutActivity" ]; then
            if [ "$package" = "$PACKAGE" ]; then
                activity_name=".ShortcutActivity"
            else
                activity_name="$package.ShortcutActivity"
            fi
            cat << EOF >> app/src/main/AndroidManifest.xml
        <activity
            android:name="$activity_name"
            android:exported="true"
            android:enabled="$enabled"
            android:theme="@android:style/Theme.$theme">
            <intent-filter>
                <action android:name="android.intent.action.CREATE_SHORTCUT" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
EOF
        elif [ "$activity" = "PermissionActivity" ]; then
            if [ "$package" = "$PACKAGE" ]; then
                activity_name=".PermissionActivity"
            else
                activity_name="$package.PermissionActivity"
            fi
            cat << EOF >> app/src/main/AndroidManifest.xml
        <activity
            android:name="$activity_name"
            android:exported="true"
            android:enabled="$enabled"
            android:theme="@android:style/Theme.$theme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
EOF
        else
            if [ "$package" = "$PACKAGE" ]; then
                activity_name=".$activity"
            else
                activity_name="$package.$activity"
            fi
            cat << EOF >> app/src/main/AndroidManifest.xml
        <activity
            android:name="$activity_name"
            android:exported="false"
            android:enabled="$enabled"
            android:theme="@android:style/Theme.$theme" />
EOF
        fi
        echo "✅ Добавлена активити: $activity ($activity_name)"
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