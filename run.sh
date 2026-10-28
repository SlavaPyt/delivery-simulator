#!/usr/bin/env bash
# Запуск без Gradle: компилирует все классы проекта и запускает программу.
# Использование: bash run.sh
cd "$(dirname "$0")" || exit 1
rm -rf out
if ! javac -encoding UTF-8 -d out $(find src/main/java -name '*.java'); then
  echo
  echo "Не удалось скомпилировать. Нужна Java 17 или новее: проверьте команду 'java -version'."
  exit 1
fi
java -Dfile.encoding=UTF-8 -cp out ru.leti.delivery.Main "$@"
