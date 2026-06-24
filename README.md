# superkassa-receipt-renderer

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/superkassa-receipt-renderer.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.texport/superkassa-receipt-renderer)
[![Version](https://img.shields.io/badge/version-1.0.1-blue.svg)](#)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/superkassa-receipt-renderer/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/superkassa-receipt-renderer/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке)

---

## Documentation in English

A modular, clean-architecture Kotlin/JVM library providing flexible HTML/text receipt rendering mechanisms for the **Superkassa** fiscalization system.

It handles rendering of sale and return receipts, cash operations (in/out), shift openings, and shift closures (Z-reports, X-reports) using modern Google Material 3 CSS styles suitable for web presentation, mobile app previews, and thermal printouts.

### Key Features
- **Trilingual Localized Templates**: Renders items, payment types, taxes, and operation headers with dual/triple translations (Russian/Kazakh/English) required for official Kazakh fiscalization.
- **Material 3 Design Guidelines**: Styled according to Google Material 3 guidelines (clean typography hierarchy, rounded containers, card outlines, pill-shaped status badges).
- **Dark/Light Theme Support**: Supports automatic theme switching based on device preferences (`prefers-color-scheme: dark`) and programmatic override classes (`body.dark` / `body.light`) for visual editors.
- **Print & Paper Roll Sizing Enforcements**:
  - Automatically forces clean black-and-white print styles in print-media queries (`@media print`) to eliminate background colors and save ink/toner.
  - Dynamically sets page layouts (`@page size`) and container dimensions for standard POS roll sizes: **80mm** (standard wide POS tape) and **58mm** (narrow mobile POS tape, auto-adjusting font size and margins to prevent squeeze and overlaps).
- **Branding & Visual Editor Support**: Includes custom header/footer HTML embedding, logo loading, custom CSS injection, and a dedicated mockup preview rendering API (`renderPreviewHtml`).
- **QR Code & Signature Integration**: Generates print-ready QR codes linking to OOFD check verification portals with dynamic domain name resolution depending on the provider (Kazakhtelecom, Transtelecom, Alteco).

---

### Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.texport:superkassa-receipt-renderer:1.0.1")
}
```

---

### Usage Example

```kotlin
import kz.mybrain.superkassa.core.data.receipt.ReceiptHtmlRenderer
import kz.mybrain.superkassa.core.domain.model.*

// 1. Instantiate renderer with a QR code generator adapter
val renderer = ReceiptHtmlRenderer(myQrCodeGeneratorImpl)

// 2. Prepare mock/real KkmInfo with branding config
val kkmInfo = KkmInfo(
    id = "kkm-123",
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
    mode = "PRODUCTION",
    state = "READY",
    registrationNumber = "RN-999999",
    factoryNumber = "FN-888888",
    branding = ReceiptBranding(
        language = ReceiptLanguage.MIXED,               // RU, KK, or MIXED
        headerLogoUrl = "https://mysite.com/logo.png",   // Optional logo
        headerHtml = "<div>Welcome to Store!</div>",     // Optional top header HTML
        footerHtml = "<div>Thank you!</div>",            // Optional bottom footer HTML
        customCss = ".doc-title { color: #6200EE; }",    // Optional CSS overrides
        paperWidthMm = 58                                // 58 or 80 (default)
    ),
    ofdServiceInfo = myOfdServiceInfo
)

// 3. Generate receipt HTML
val htmlString = renderer.renderHtml(myReceiptRequest, myFiscalDocumentSnapshot, kkmInfo)

// 4. Render HTML in WebView or send to PDF converter
myWebView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
```

### Visual Preview API (For Branding Configurator UI)

If you are building a visual branding configurator UI editor, you can generate an instant preview of a mock receipt styled with the current draft branding settings:

```kotlin
val draftBranding = ReceiptBranding(
    language = ReceiptLanguage.RU,
    paperWidthMm = 58
)
val previewHtml = renderer.renderPreviewHtml(draftBranding)
```

---

## Документация на русском языке

Модульная библиотека на Kotlin/JVM для отрисовки печатных форм (HTML) фискальных чеков и отчетов системы **Superkassa**.

Библиотека отвечает за формирование HTML-страниц для чеков продажи/возврата, операций инкассации/внесения наличных, открытия смены и закрытия смены (Z-отчеты, X-отчеты) с адаптивной версткой под Google Material 3.

### Ключевые возможности
- **Двуязычные локализованные шаблоны**: Автоматическая отрисовка наименований товаров, типов оплат, ставок НДС и мета-информации на русском и казахском языках в соответствии с требованиями КГД РК.
- **Дизайн по канонам Material 3**: Соответствие современным гайдлайнам Google Material 3 (скругления контейнеров, четкие шрифты, плоские M3 разделители, скругленные pill-статусы).
- **Светлая и темная темы**: Поддержка автоматического переключения тем на уровне браузера (`prefers-color-scheme: dark`) и ручного форсирования классов (`body.dark` / `body.light`) для Web-интерфейсов визуальных редакторов.
- **Оптимизация под чековые ленты и печать**:
  - Автоматический сброс фонов и темных цветов в белый/черный режим при печати (`@media print`) для экономии тонера и сохранения читаемости на бумаге.
  - Поддержка двух форматов лент: **80мм** (стандартная широкая лента) и **58мм** (мобильная узкая лента). Для 58мм автоматически сжимаются шрифты, отступы и таблицы, исключая выползание цифр и перенос колонок.
- **Визуальное брендирование и Live Preview**: Возможность гибкой настройки логотипа, вставки кастомного HTML в шапку и подвал, инжекции произвольных CSS стилей и вызова предпросмотра макета через `renderPreviewHtml`.
- **Динамический выбор ОФД**: Автоматическая генерация QR-кода и ссылок проверки с динамическим выбором доменов в зависимости от провайдера ОФД (`o.oofd.kz` для Транстелекома, `alteco.kz` для Alteco, `consumer.oofd.kz` для Казахтелекома).

---

### Установка

Добавьте зависимость в ваш `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.texport:superkassa-receipt-renderer:1.0.1")
}
```

---

### Пример использования

```kotlin
import kz.mybrain.superkassa.core.data.receipt.ReceiptHtmlRenderer
import kz.mybrain.superkassa.core.domain.model.*

// 1. Инициализация рендерера с адаптером QR-кодов
val renderer = ReceiptHtmlRenderer(myQrCodeGeneratorImpl)

// 2. Подготовка настроек брендирования ККМ
val kkmInfo = KkmInfo(
    id = "kkm-123",
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
    mode = "PRODUCTION",
    state = "READY",
    registrationNumber = "RN-999999",
    factoryNumber = "FN-888888",
    branding = ReceiptBranding(
        language = ReceiptLanguage.MIXED,               // Язык чека: RU, KK, или MIXED
        headerLogoUrl = "https://mysite.com/logo.png",   // Ссылка на логотип
        headerHtml = "<div>Добро пожаловать!</div>",     // Кастомный верхний колонтитул
        footerHtml = "<div>Спасибо за покупку!</div>",   // Кастомный нижний колонтитул
        customCss = ".doc-title { color: #6200EE; }",    // Произвольные CSS-стили
        paperWidthMm = 58                                // Ширина ленты: 58 или 80 (по умолчанию)
    ),
    ofdServiceInfo = myOfdServiceInfo
)

// 3. Генерация печатной формы
val htmlString = renderer.renderHtml(myReceiptRequest, myFiscalDocumentSnapshot, kkmInfo)

// 4. Отображение в WebView или отправка на принтер
webView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
```

### API Предпросмотра (Для Конструктора Брендирования)

Если вы разрабатываете UI-редактор брендирования чеков, вы можете быстро сгенерировать готовый макет чека с тестовым набором товаров для отображения в WebView/iframe:

```kotlin
val draftBranding = ReceiptBranding(
    language = ReceiptLanguage.KK,
    paperWidthMm = 58
)
val previewHtml = renderer.renderPreviewHtml(draftBranding)
```
