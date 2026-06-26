# superkassa-receipt-renderer

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/superkassa-receipt-renderer.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.texport/superkassa-receipt-renderer)
[![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)](#)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](#)
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
- **Print & Sizing Layouts**:
  - Automatically forces clean black-and-white print styles in print-media queries (`@media print`) to eliminate background colors and save ink/toner.
  - Dynamically sets page layouts (`@page size`) and container dimensions for standard layouts: **80mm** (standard wide POS tape), **58mm** (narrow mobile POS tape, auto-adjusting font size and margins to prevent squeeze and overlaps), and **Fullscreen** mode (for borderless, seamless integration into mobile/web preview screens).
- **Expanded Branding & Visual Editor Support**: Includes custom slots for embedding HTML fragments, custom color accent override (`themeColor`), logo loading, custom CSS injection, and a dedicated mockup preview rendering API (`renderPreviewHtml`).
- **Dynamic OFD Registry**: Supports custom OFD providers mapping to resolve taxpayer company names and verify document domains dynamically via configurations.

---

### Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.texport:superkassa-receipt-renderer:1.0.2")
}
```

---

### Usage Example

#### 1. Receipts Rendering (Sale / Return / Buy / Refund Buy)

```kotlin
import kz.mybrain.superkassa.core.data.receipt.ReceiptHtmlRenderer
import kz.mybrain.superkassa.core.domain.model.*

// 1. Instantiate renderer with a QR code generator adapter
val renderer = ReceiptHtmlRenderer(
    qrCodeGenerator = myQrCodeGeneratorImpl,
    ofdProviders = ReceiptHtmlRenderer.defaultOfdProviders // customize if needed
)

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
        language = ReceiptLanguage.MIXED,               // RU, KK, or MIXED (Kazakh first)
        paperWidthMm = 58,                               // 58 (narrow POS tape) or 80 (default)
        themeColor = "indigo",                           // Accent color key (indigo, teal, green, blue, orange, rose)
        headerLogoUrl = "https://mysite.com/logo.png",   // Optional logo URL
        
        // Custom branding message slots (Clean Architecture compatible text, safe HTML wrap)
        beforeHeaderMsg = "TOP AD BLOCK",
        headerMsg = "Welcome to Store!",
        afterHeaderMsg = "Sub-header info",
        beforeItemsMsg = "Items list below:",
        afterItemsMsg = "Items list above",
        beforeTotalsMsg = "Promo: 10% applied",
        afterTotalsMsg = "Refund terms & info",
        beforeQrMsg = "Scan to verify check",
        footerMsg = "Thank you for choosing us!",
        
        useForceDarkTheme = false,                       // Dynamic theme controls
        customBackgroundColorHex = "#faf8f5",
        customCardTopBorderColorHex = "#4f46e5"
    ),
    ofdServiceInfo = myOfdServiceInfo
)

// 3. Generate receipt HTML
val htmlString = renderer.renderHtml(myReceiptRequest, myFiscalDocumentSnapshot, kkmInfo)

// 4. Render HTML in WebView or send to PDF converter
myWebView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
```

#### 2. X-Reports and Z-Reports Rendering

```kotlin
val xReportHtml = renderer.renderXReportHtml(
    shift = myShiftInfo,
    counters = myCountersMap,
    kkm = kkmInfo,
    ofdStatus = "DELIVERED" // or "OFFLINE", "ERROR"
)

val zReportHtml = renderer.renderCloseShiftHtml(
    shift = myShiftInfo,
    counters = myCountersMap,
    kkm = kkmInfo,
    ofdStatus = "DELIVERED"
)
```

#### 3. Cash Operations (Cash In / Cash Out)

```kotlin
val cashOpHtml = renderer.renderCashOperationHtml(
    doc = myFiscalDocumentSnapshot,
    kkm = kkmInfo
)
```

#### 4. Shift Openings

```kotlin
val openShiftHtml = renderer.renderOpenShiftHtml(
    shift = myShiftInfo,
    kkm = kkmInfo,
    ofdStatus = "DELIVERED"
)
```

### Visual Preview API (For Branding Configurator UI)

If you are building a visual branding configurator UI editor, you can generate an instant preview of a mock receipt styled with the current draft branding settings:

```kotlin
val draftBranding = ReceiptBranding(
    language = ReceiptLanguage.MIXED,
    paperWidthMm = 58,
    themeColor = "#ff5722",
    headerHtml = "<div>Store Preview</div>"
)
val previewHtml = renderer.renderPreviewHtml(draftBranding)
```

---

## Документация на русском языке

Модульная библиотека на Kotlin/JVM для отрисовки печатных форм (HTML) фискальных чеков и отчетов системы **Superkassa**.

Библиотека отвечает за формирование HTML-страниц для чеков продажи/возврата, операций инкассации/внесения наличных, открытия смены и закрытия смены (Z-отчеты, X-отчеты) с адаптивной версткой под Google Material 3.

### Ключевые возможности
- **Двуязычные локализованные шаблоны**: Автоматическая отрисовка наименований товаров, типов оплат, ставок НДС и мета-информации на русском и казахском языках в соответствии с требованиями КГД РК. В смешанном режиме (`MIXED`) казахский язык идет приоритетным (первым).
- **Дизайн по канонам Material 3**: Соответствие современным гайдлайнам Google Material 3 (скругления контейнеров, четкие шрифты, плоские M3 разделители, скругленные pill-статусы).
- **Светлая и темная темы**: Поддержка автоматического переключения тем на уровне браузера (`prefers-color-scheme: dark`) и ручного форсирования классов (`body.dark` / `body.light`) для Web-интерфейсов визуальных редакторов.
- **Оптимизация под макеты и печать**:
  - Автоматический сброс фонов и темных цветов в белый/черный режим при печати (`@media print`) для экономии тонера и сохранения читаемости на бумаге.
  - Поддержка трех форматов макета: **80мм** (стандартная широкая лента), **58мм** (мобильная узкая лента с автоматическим масштабированием и вертикальным стекированием заголовков) и **Полноэкранный режим** (без полей и скроллбаров, для бесшовной интеграции в визуальные интерфейсы веб-кабинетов и мобильных приложений).
- **Визуальное брендирование и Live Preview**: Настройка логотипа, выбор кастомного цветового акцента (`themeColor`), вставка HTML-фрагментов во все ключевые секции чека, инжекция произвольных CSS-стилей и вызов предпросмотра макета через `renderPreviewHtml`.
- **Динамический выбор ОФД**: Автоматическая генерация QR-кода и ссылок с динамическим разрешением доменов проверки чеков на основе зарегистрированных в системе провайдеров ОФД без их жесткого прописывания в коде.

---

### Установка

Добавьте зависимость в ваш `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.texport:superkassa-receipt-renderer:1.0.2")
}
```

---

### Пример использования

#### 1. Генерация чеков (Продажа / Возврат / Покупка / Возврат покупки)

```kotlin
import kz.mybrain.superkassa.core.data.receipt.ReceiptHtmlRenderer
import kz.mybrain.superkassa.core.domain.model.*

// 1. Инициализация рендерера с адаптером QR-кодов
val renderer = ReceiptHtmlRenderer(
    qrCodeGenerator = myQrCodeGeneratorImpl,
    ofdProviders = ReceiptHtmlRenderer.defaultOfdProviders // реестр провайдеров
)

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
        language = ReceiptLanguage.MIXED,               // Язык чека: RU, KK, или MIXED (казахский идет первым)
        paperWidthMm = 58,                               // Ширина ленты: 58 или 80 (по умолчанию)
        themeColor = "indigo",                           // Цветовой акцент чека (indigo, teal, green, blue, orange, rose)
        headerLogoUrl = "https://mysite.com/logo.png",   // Ссылка на логотип
        
        // Слоты для кастомных текстовых сообщений (Clean Architecture)
        beforeHeaderMsg = "РЕКЛАМНЫЙ БЛОК НАВЕРХУ",
        headerMsg = "Добро пожаловать!",
        afterHeaderMsg = "Адрес магазина или режим работы",
        beforeItemsMsg = "Список товаров ниже:",
        afterItemsMsg = "Список товаров выше",
        beforeTotalsMsg = "Промокод на скидку 10%",
        afterTotalsMsg = "Информация по условиям возврата",
        beforeQrMsg = "Отсканируйте для проверки",
        footerMsg = "Спасибо за покупку!",
        
        useForceDarkTheme = false,                       // Управление темой оформления
        customBackgroundColorHex = "#faf8f5",
        customCardTopBorderColorHex = "#4f46e5"
    ),
    ofdServiceInfo = myOfdServiceInfo
)

// 3. Генерация печатной формы
val htmlString = renderer.renderHtml(myReceiptRequest, myFiscalDocumentSnapshot, kkmInfo)

// 4. Отображение в WebView или отправка на принтер
webView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
```

#### 2. Генерация X-отчетов и Z-отчетов (Закрытие смены)

```kotlin
val xReportHtml = renderer.renderXReportHtml(
    shift = myShiftInfo,
    counters = myCountersMap,
    kkm = kkmInfo,
    ofdStatus = "DELIVERED" // "OFFLINE", "ERROR"
)

val zReportHtml = renderer.renderCloseShiftHtml(
    shift = myShiftInfo,
    counters = myCountersMap,
    kkm = kkmInfo,
    ofdStatus = "DELIVERED"
)
```

#### 3. Кассовые операции (Внесение / Изъятие наличных)

```kotlin
val cashOpHtml = renderer.renderCashOperationHtml(
    doc = myFiscalDocumentSnapshot,
    kkm = kkmInfo
)
```

#### 4. Открытие смены

```kotlin
val openShiftHtml = renderer.renderOpenShiftHtml(
    shift = myShiftInfo,
    kkm = kkmInfo,
    ofdStatus = "DELIVERED"
)
```

### API Предпросмотра (Для Конструктора Брендирования)

Если вы разрабатываете UI-редактор брендирования чеков, вы можете быстро сгенерировать готовый макет чека с тестовым набором товаров для отображения в WebView/iframe:

```kotlin
val draftBranding = ReceiptBranding(
    language = ReceiptLanguage.KK,
    paperWidthMm = 58,
    themeColor = "#e91e63",
    headerHtml = "<div>Тестовый чек предпросмотра</div>"
)
val previewHtml = renderer.renderPreviewHtml(draftBranding)
```
