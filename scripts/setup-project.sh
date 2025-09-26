#!/bin/bash
# Проверка файлов
for file in "$ICON_LAUNCHER" "$ICON_SHORTCUT" "$ICON_NO_ICON"; do
    [ -f "icons/$file" ] || { echo "❌ Файл не найден: icons/$file"; ls -la icons; exit 1; }
    echo "✅ Найден: icons/$file"
done

for kotlin_file in src/*.kt; do
    [ -f "$kotlin_file" ] && echo "✅ Найден: $kotlin_file"
done

# Создание директорий
mkdir -p app

[ -d "app" ] || { echo "❌ Не удалось создать папку app/"; exit 1; }
JAVA_PATH=$(echo "$PACKAGE" | tr '.' '/')
mkdir -p app/src/main/java/$JAVA_PATH app/src/main/res/{values,layout,mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}} app/src/main/assets

# Keystore
echo "✅ Создание debug keystore..."
keytool -genkeypair -v -keystore app/debug.keystore -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=Debug,OU=Debug,O=Debug,L=Debug,ST=Debug,C=US" 2>/dev/null
[ -f "app/debug.keystore" ] && echo "✅ Keystore создан" || { echo "❌ Ошибка keystore"; exit 1; }

# Обработка .kt
for kotlin_file in src/*.kt; do
    [ -f "$kotlin_file" ] || continue
    base_name=$(basename "$kotlin_file")
    cp "$kotlin_file" "/tmp/${base_name}.tmp"
    sed -i '/^package /d' "/tmp/${base_name}.tmp"
    sed -i '/./,$!d' "/tmp/${base_name}.tmp"
    cp "/tmp/${base_name}.tmp" "app/src/main/java/$JAVA_PATH/$base_name"
    sed -i "1i package $PACKAGE" "app/src/main/java/$JAVA_PATH/$base_name"
    rm "/tmp/${base_name}.tmp"
    echo "✅ Очищен: $base_name"
done

# Копирование иконок
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    cp "icons/$ICON_LAUNCHER" app/src/main/res/mipmap-$density/ic_launcher.png
    cp "icons/$ICON_SHORTCUT" app/src/main/res/mipmap-$density/ic_shortcut.png
    cp "icons/$ICON_NO_ICON" app/src/main/res/mipmap-$density/ic_no_icon.png
done

# Копирование дополнительных иконок из icons/
if [ -d "icons" ]; then
    for icon in $(find icons -name "*.png" ! -name "$ICON_LAUNCHER" ! -name "$ICON_SHORTCUT" ! -name "$ICON_NO_ICON"); do
        icon_name=$(basename "$icon")
        for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
            cp "$icon" "app/src/main/res/mipmap-$density/$icon_name"
            echo "✅ Копирована иконка: $icon_name в mipmap-$density"
        done
    done
else
    echo "❌ Папка icons/ не найдена"
    exit 1
fi

# Копирование всех файлов из res/ в assets/
if [ -d "res" ]; then
    cp -r res/* app/src/main/assets/ 2>/dev/null || true
    echo "✅ Копированы ресурсы из res/ в assets/"
else
    echo "⚠️ Папка res/ не найдена"
fi

echo "✅ Проект подготовлен"