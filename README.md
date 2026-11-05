# Prompt Editor v2.2

Десктопное приложение для редактирования промтов для нейросетей (LLM).

## Стек

- **Язык**: Kotlin 2.x
- **UI**: JavaFX 21 (LTS)
- **JDK**: 21 LTS
- **Сборка**: Gradle Kotlin DSL
- **JSON**: Jackson (jackson-module-kotlin)
- **Текст с подсветкой**: RichTextFX
- **Орфография**: JOrtho
- **Тесты**: JUnit 5

## Установка и запуск

### Требования

- JDK 21 или выше
- Gradle 8.x (включён через wrapper)

### Сборка и запуск

```bash
# Сборка проекта
./gradlew build

# Запуск приложения
./gradlew run
```

### Упаковка через jpackage

#### Linux (.deb)

```bash
# Сборка JAR
./gradlew jar

# Упаковка в deb
jpackage \
  --input build/libs \
  --name prompt-editor \
  --main-jar prompteditor-2.2.0.jar \
  --main-class com.pandakind.prompteditor.PromptEditorApp \
  --type deb \
  --app-version 2.2 \
  --vendor "pandakind" \
  --description "Prompt Editor - редактор промтов для нейросетей" \
  --linux-shortcut \
  --linux-menu-group "Development"
```

#### Windows (.msi)

```powershell
# Сборка JAR
./gradlew jar

# Упаковка в MSI
jpackage `
  --input build\libs `
  --name prompt-editor `
  --main-jar prompteditor-2.2.0.jar `
  --main-class com.pandakind.prompteditor.PromptEditorApp `
  --type msi `
  --app-version 2.2 `
  --vendor "pandakind" `
  --description "Prompt Editor - редактор промтов для нейросетей" `
  --win-shortcut `
  --win-menu `
  --win-dir-chooser
```

#### macOS (.dmg)

```bash
jpackage \
  --input build/libs \
  --name prompt-editor \
  --main-jar prompteditor-2.2.0.jar \
  --main-class com.pandakind.prompteditor.PromptEditorApp \
  --type dmg \
  --app-version 2.2 \
  --mac-package-name "PromptEditor" \
  --mac-package-identifier "com.pandakind.prompteditor" \
  --mac-app-category "public.app-category.developer-tools"
```

## Структура проекта

```
src/main/kotlin/com/pandakind/prompteditor/
├── PromptEditorApp.kt          # Точка входа
├── controller/
│   ├── ProfileController.kt    # Управление профилями
│   ├── PromptController.kt     # Сборка и валидация промта
│   └── SpellCheckerService.kt  # Проверка орфографии
├── model/
│   ├── Profile.kt              # Модель профиля
│   ├── Section.kt              # Модель раздела
│   └── SectionManager.kt       # Управление разделами
├── util/
│   └── AppPaths.kt             # ОС-специфичные пути
└── view/
    ├── MainWindow.kt           # Главное окно
    ├── SectionEditorDialog.kt  # Диалог редактирования разделов
    ├── SpellCheckedCodeArea.kt # Текстовое поле с проверкой орфографии
    ├── TabEditorView.kt        # Вкладка редактора раздела
    └── TemplatesPanelView.kt   # Панель шаблонных формулировок
```

## Функциональные возможности

- **Динамические разделы**: добавление, удаление, редактирование, перемещение
- **Профили**: сохранение, загрузка, автозагрузка последнего профиля
- **Шаблонные формулировки**: добавление, удаление, вставка в текст
- **Проверка орфографии**: debounce, подсветка ошибок, контекстное меню
- **Сборка промта**: формирование итогового текста из разделов
- **Импорт/экспорт**: JSON (профили), TXT (промты)
- **Сброс данных**: с подтверждением

## Тесты

```bash
# Запуск всех тестов
./gradlew test

# Запуск с отчётом
./gradlew test --info
```

## CI/CD

GitHub Actions настроен в `.github/workflows/build.yml`:

- **Push/Pull Request**: сборка и тесты на Ubuntu и Windows
- **Release**: автоматическая упаковка в `.deb` (Linux) и `.msi` (Windows)

## Хранилище данных

Данные пользователя хранятся в ОС-специфичных директориях:

- **Linux**: `~/.config/prompt_editor/`
- **Windows**: `%APPDATA%\prompt_editor\`
- **macOS**: `~/Library/Application Support/prompt_editor/`

Внутри:
- `profiles/` — файлы профилей (.json)
- `user_dict/user_dictionary.txt` — пользовательский словарь
- `last_profile.txt` — имя последнего использованного профиля

## Лицензия

MIT
