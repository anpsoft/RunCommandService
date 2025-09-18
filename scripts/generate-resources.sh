#!/bin/bash
# AndroidManifest.xml
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
[ "$MAIN_ENABLED" = "true" ] && cat >> app/src/main/AndroidManifest.xml << EOF
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
EOF
[ "$SHORTCUT_ENABLED" = "true" ] && cat >> app/src/main/AndroidManifest.xml << EOF
        <activity
            android:name=".ShortcutActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Translucent.NoTitleBar"
            android:excludeFromRecents="true"/>
EOF
[ "$SILENT_ENABLED" = "true" ] && cat >> app/src/main/AndroidManifest.xml << EOF
        <activity
            android:name=".SilentActivity"
            android:exported="false"
            android:theme="@android:style/Theme.NoDisplay"
            android:excludeFromRecents="true"/>
EOF
cat >> app/src/main/AndroidManifest.xml << EOF
        <activity
            android:name=".ScriptSettingsActivity"
            android:exported="false"
            android:theme="@style/$MAIN_THEME"/>
    </application>
</manifest>
EOF

# Копирование XML из templates/
if [ -d "templates/values" ]; then
    for xml in $(find templates/values -name "*.xml"); do
        xml_name=$(basename "$xml")
        envsubst '$APP_NAME,$MAIN_THEME' < "$xml" > "app/src/main/res/values/$xml_name"
        echo "✅ Копирован: $xml_name в values"
    done
fi
if [ -d "templates/layout" ]; then
    for xml in $(find templates/layout -name "*.xml"); do
        xml_name=$(basename "$xml")
        envsubst '$APP_NAME,$MAIN_THEME' < "$xml" > "app/src/main/res/layout/$xml_name"
        echo "✅ Копирован: $xml_name в layout"
    done
fi

echo "✅ Ресурсы сгенерированы"