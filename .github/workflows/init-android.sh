#!/bin/bash
echo "=== 🚀 НАЧАЛО СБОРКИ ==="
echo "Текущая директория: $(pwd)"
ls -la .

chmod +x scripts/*.sh
source scripts/parse-config.sh
bash scripts/setup-project.sh
bash scripts/generate-resources.sh
bash scripts/gradle_init.sh

echo "=== 🔍 ПРОВЕРКА СТРУКТУРЫ ==="
# Показать структуру, исключая .git, с древовидным форматом
find . -path ./.git -prune -o -print | sort | sed 's/[^\/]*\//|   /g;s/|   \([^|]\)/|___ \1/g'

echo "=== 🔍 ПРОВЕРКА РЕСУРСОВ ==="
find app/src/main/res -name "*.png" -type f

[ -f "app/src/main/res/values/strings.xml" ] && echo "✅ strings.xml: есть" || echo "❌ strings.xml: отсутствует"
[ -f "app/src/main/res/mipmap-mdpi/ic_launcher.png" ] && echo "✅ ic_launcher.png: есть" || echo "❌ ic_launcher.png: отсутствует"
[ -f "app/src/main/java/$JAVA_PATH/MainActivity.kt" ] && echo "✅ MainActivity.kt: есть" || echo "❌ MainActivity.kt: отсутствует"
[ -f "app/build.gradle" ] && echo "✅ build.gradle: сгенерирован" || echo "❌ build.gradle: отсутствует"
[ -f "app/src/main/AndroidManifest.xml" ] && echo "✅ AndroidManifest.xml: сгенерирован" || echo "❌ AndroidManifest.xml: отсутствует"

echo "✅ Инициализация завершена"