#!/bin/bash
# Генерация AndroidManifest.xml
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