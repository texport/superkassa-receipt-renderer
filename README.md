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

It handles rendering of sale and return receipts, cash operations (in/out), shift openings, and shift closures (Z-reports, X-reports) using modern CSS styles suitable for web presentation, mobile app previews, and thermal printouts.

### Key Features
- **Trilingual Localized Templates**: Renders items, payment types, taxes, and operation headers with dual/triple translations (Russian/Kazakh/English) required for official Kazakh fiscalization.
- **Embedded Style Sheets**: Comes with built-in responsive CSS styles (`ReceiptHtmlStyles.CSS`) supporting desktop display, web view frame previews, and standard thermal paper roll sizes (e.g., 58mm / 80mm).
- **QR Code & Signature Integration**: Generates print-ready QR codes linking to OOFD (Kazakh Telecom, Transtelecom, Alteco) check verification sites.
- **Decoupled Business Logic**: Does not depend on specific print hardware or UI engines. Input parameters are cleanly defined via core DOM entities (`ReceiptRequest`, `FiscalDocumentSnapshot`, `ShiftInfo`).

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
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest

// 1. Instantiate renderer with a QR code generator adapter
val renderer = ReceiptHtmlRenderer(myQrCodeGeneratorImpl)

// 2. Generate check HTML
val htmlString = renderer.renderHtml(myReceiptRequest, myFiscalDocumentSnapshot)

// 3. Render HTML in WebView or send to printer
myWebView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
```

---

## Документация на русском языке

Модульная библиотека на Kotlin/JVM для отрисовки печатных форм (HTML) фискальных чеков и отчетов системы **Superkassa**.

Библиотека отвечает за формирование HTML-страниц для чеков продажи/возврата, операций инкассации/внесения наличных, открытия смены и закрытия смены (Z-отчеты, X-отчеты) с адаптивной версткой под стандартную чековую ленту.

### Ключевые возможности
- **Двуязычные локализованные шаблоны**: Автоматическая отрисовка наименований товаров, типов оплат, ставок НДС и мета-информации на русском и казахском языках в соответствии с требованиями КГД РК.
- **Встроенные CSS Стили**: Поставляется с готовым набором стилей (`ReceiptHtmlStyles.CSS`), адаптированных под мобильные экраны, WebView и печать на термопринтере (сброс фонов в print-медиа выражении).
- **Интеграция QR-кодов**: Поддержка динамической генерации QR-кода со ссылкой на ОФД (Казахтелеком, Транстелеком, Alteco) для мгновенной проверки фискального чека покупателем.
- **Изолированная логика**: Шаблонизатор работает исключительно с сущностями ядра (`ReceiptRequest`, `FiscalDocumentSnapshot`), что позволяет переиспользовать код на сервере и в мобильном SDK.

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
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest

// 1. Инициализация рендерера с адаптером QR-кодов
val renderer = ReceiptHtmlRenderer(myQrCodeGeneratorImpl)

// 2. Генерация печатной формы
val htmlString = renderer.renderHtml(myReceiptRequest, myFiscalDocumentSnapshot)

// 3. Отображение в WebView или отправка на принтер
webView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
```
