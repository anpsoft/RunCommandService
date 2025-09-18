#!/bin/bash
APP_INI_PATH="${1:-app.ini}"
DEFAULT_COMPILE_SDK=34
DEFAULT_TARGET_SDK=34
DEFAULT_MIN_SDK=21
if [ ! -f "$APP_INI_PATH" ]; then
  echo "❌ ОШИБКА: Файл app.ini не найден"
  APP_INI_PATH="../RunCommandService/app.ini"
  [ -f "$APP_INI_PATH" ] || { echo "❌ app.ini не найден"; exit 1; }
fi
echo "✅ Найден app.ini: $APP_INI_PATH"
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
export COMPILE_SDK=${config["SDK_compileSdk"]:-$DEFAULT_COMPILE_SDK}
export TARGET_SDK=${config["SDK_targetSdk"]:-$DEFAULT_TARGET_SDK}
export MIN_SDK=${config["SDK_minSdk"]:-$DEFAULT_MIN_SDK}
export APP_NAME=${config["Common_appName"]:-YourApp}
export PACKAGE=${config["Common_package"]:-com.yourcompany.yourapp3}
export VERSION_CODE=${config["Common_versionCode"]:-1}
export VERSION_NAME=${config["Common_versionName"]:-1.0}
export MAIN_ACTIVITY_PATH=${config["Common_mainActivityPath"]:-MainActivity.kt}

export ICON_LAUNCHER=${config["Common_iconlauncher"]:-icon.png}
export ICON_SHORTCUT=${config["Common_iconShortcut"]:-Terminal.png}
export ICON_NO_ICON=${config["Common_iconNoIcon"]:-no_icon.png}

export BUILD_TYPE=${config["Common_buildType"]:-debug}
export MAIN_ENABLED=${config["MainActivity_enabled"]:-true}
export MAIN_THEME=${config["MainActivity_theme"]:-AppTheme}
export SHORTCUT_ENABLED=${config["ShortcutActivity_enabled"]:-true}
export SHORTCUT_THEME=${config["ShortcutActivity_theme"]:-Translucent}
export SHORTCUT_TOASTS=${config["ShortcutActivity_showToasts"]:-true}
export SILENT_ENABLED=${config["SilentActivity_enabled"]:-false}
export SILENT_THEME=${config["SilentActivity_theme"]:-NoDisplay}
echo "✅ Конфигурация загружена"