#!/bin/bash

# Читаем значения из ini
PACKAGE=$(grep "package=" build.ini | cut -d'=' -f2)
APP_NAME=$(grep "appName=" build.ini | cut -d'=' -f2)
SHORTCUT_ENABLED=$(grep -A5 "\[ShortcutActivity\]" build.ini | grep "enabled=" | cut -d'=' -f2)
SILENT_ENABLED=$(grep -A5 "\[SilentActivity\]" build.ini | grep "enabled=" | cut -d'=' -f2)

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
        <activity
            android:name=".PermissionActivity"
            android:exported="true"
            android:enabled="true"
            android:theme="@android:style/Theme.DeviceDefault.Light">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name=".MainActivity"
            android:exported="false"
            android:enabled="true"
            android:theme="@android:style/Theme.DeviceDefault.Light" />
        <activity
            android:name=".ScriptSettingsActivity"
            android:exported="false"
            android:theme="@android:style/Theme.DeviceDefault.Light" />
EOF

# Автоматически добавляем все активити из ini если enabled=true
for activity in AboutActivity InstructionsActivity SettingsActivity; do
    enabled=$(grep -A5 "\[$activity\]" build.ini 2>/dev/null | grep "enabled=" | cut -d'=' -f2)
    if [ "$enabled" = "true" ]; then
        echo "        <activity" >> app/src/main/AndroidManifest.xml
        echo "            android:name=\".$activity\"" >> app/src/main/AndroidManifest.xml
        echo "            android:exported=\"false\"" >> app/src/main/AndroidManifest.xml
        echo "            android:theme=\"@android:style/Theme.DeviceDefault.Light\" />" >> app/src/main/AndroidManifest.xml
    fi
done

# Добавляем остальные активити
cat << EOF >> app/src/main/AndroidManifest.xml
        <activity
            android:name=".ShortcutActivity"
            android:exported="true"
            android:enabled="$SHORTCUT_ENABLED"
            android:theme="@android:style/Theme.Translucent.NoTitleBar">
            <intent-filter>
                <action android:name="android.intent.action.CREATE_SHORTCUT" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
        <activity
            android:name=".SilentActivity"
            android:exported="false"
            android:enabled="$SILENT_ENABLED"
            android:theme="@android:style/Theme.NoDisplay" />
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
cat -n app/src/main/AndroidManifest.xml
echo "=================================="
echo "✅ AndroidManifest.xml создан успешно"
echo "=================================="