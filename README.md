# PaperTrade — Android securities UI

A native Java Android portfolio project with a Webull-inspired dark interface and an original PaperTrade identity. All prices, charts, funds and executions are **simulated**. No brokerage connection, live data, or real-money transactions.

## Screens

- **Markets:** index cards, featured stock, searchable six-stock market list and sparklines.
- **Watchlist:** persisted favorites; add or remove a stock from its detail page.
- **Stock detail:** synthetic line/candlestick chart, six illustrative ranges, quote details and buy/sell actions.
- **Order ticket:** quantity validation, estimated total, review confirmation and immediate simulated fills.
- **Portfolio:** virtual cash, positions, average cost and unrealized profit/loss.
- **Orders:** empty state and persisted filled-order history.

Design: near-black canvas, slate panels, lime primary actions, mint gains, rose losses, restrained borders and tabular price figures. Native scrolling, system insets, labeled controls and four-tab navigation.

## Open and run

1. Install Android Studio with Android SDK Platform 35 and JDK 17.
2. Open this repository as a Gradle project and let Android Studio sync.
3. Run the **app** configuration on an Android 10 (API 29) or newer emulator/device.

Build tooling: Android Gradle Plugin 8.9.2 and Gradle 8.11.1. The official Gradle wrapper is included with a pinned distribution checksum. On Windows, use `gradlew.bat`.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

GitHub Actions builds the debug APK, runs domain and Robolectric UI tests, runs Android lint, and uploads the APK, reports and rendered UI screenshots as **papertrade-build**. Download it from the successful Actions run. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Implementation

- `MainActivity`: native Android view-based screens, navigation and review flow.
- `ChartView`: Canvas charts generated deterministically; the range selector changes the illustration, not real historical data.
- `DemoBroker`: plain Java demo account model, integer-cent money, synchronized execution, insufficient-funds/oversell checks and request-ID deduplication.
- `DemoStore`: local JSON snapshot in private SharedPreferences. An order is applied to a candidate account; the UI only adopts it after persistence succeeds.

Seed: $24,750 cash plus 12 NVDA, 8 AAPL and 5 MSFT shares. The resulting total account value and unrealized P&L are computed from those holdings. All values are examples, not current market quotes. App data persists across restarts; clear app storage to reset.

## Verification

JUnit covers balance/holding changes, rejected orders without mutation, duplicate requests and competing buys. Robolectric covers navigation, search/empty results, chart controls, activity recreation, order validation, confirmation and persistence; it exports screenshots for visual review.

Manual device checklist:
- Open every tab and stock, scroll to the bottom, try a large system font.
- Search by ticker and company, including an unmatched query.
- Toggle a favorite; restart and check it remains changed.
- Switch chart ranges and line/candle modes.
- Buy two shares, confirm, and check cash/holdings/order history.
- Reject insufficient cash, overselling, blank and zero quantities.
- Cancel an order, rotate the device and verify there is no unintended fill.

## Scope and next steps

This is a GUI prototype with a local demonstration model, **not the Spring Boot/PostgreSQL backend**. There is no authentication, server-side ledger, exchange matching, partial fill, real-time quote feed or production-grade order service. Device preferences and in-process synchronization are not substitutes for server-side transactional storage.

The next portfolio milestone is replacing the local model with a Java REST service and PostgreSQL transactions while preserving this UI. Production validation must also cover authorization, durable idempotency, concurrency across clients and recovery.

Visual reference: [Webull trading platforms](https://www.webull.com/trading-platforms). No Webull logos, screenshots or proprietary assets are bundled.

