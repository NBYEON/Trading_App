# PaperTrade — Android securities UI

A Java Android client with a Webull-inspired dark interface, a Spring Boot API, and PostgreSQL account storage. All prices, charts, funds and executions are **simulated**. No brokerage connection, live data, or real-money transactions.

## Screens

- **Markets:** index cards, featured stock, searchable six-stock market list and sparklines.
- **Watchlist:** persisted favorites; add or remove a stock from its detail page.
- **Stock detail:** synthetic line/candlestick chart, six illustrative ranges, quote details and buy/sell actions.
- **Order ticket:** quantity validation, estimated total, review confirmation and server-recorded simulated fills.
- **Portfolio:** virtual cash, positions, average cost and unrealized profit/loss.
- **Orders:** empty state and persisted filled-order history.

Design: near-black canvas, slate panels, lime primary actions, mint gains, rose losses, restrained borders and tabular price figures. Native scrolling, system insets, labeled controls and four-tab navigation.

## Open and run

1. Install Android Studio with Android SDK Platform 35 and JDK 17, plus Maven and PostgreSQL 17. Docker Desktop can start PostgreSQL using the included Compose file.
2. Start PostgreSQL. With Docker, run `docker compose up -d db`. For an existing PostgreSQL installation, create a `papertrade` database and user and set `DB_URL`, `DB_USER`, and `DB_PASSWORD` for the API.
3. Start the API with `mvn -f backend/pom.xml spring-boot:run`. Flyway creates the schema and one demo account. Verify `http://localhost:8080/api/stocks` in your browser.
4. Open the repository root in Android Studio as a Gradle project, wait for sync, then run the **app** configuration on an Android 10 (API 29) or newer emulator.
5. The Android emulator connects to the server at `http://10.0.2.2:8080/api`. Its `localhost` is the emulator, not your computer. For a physical phone, set `papertradeApiUrl` to your computer's reachable address and configure the server to listen on that interface; use HTTPS outside a trusted development network.

Android tooling: Android Gradle Plugin 8.9.2 and Gradle 8.11.1. The official Gradle wrapper is included with a pinned distribution checksum. On Windows, use `gradlew.bat`. Backend tooling: Spring Boot 4.1.1, Java 17 and Maven.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
mvn -f backend/pom.xml verify
```

GitHub Actions builds the debug APK, runs Android unit and Robolectric UI tests, runs Android lint, and tests the Spring Boot service against PostgreSQL. Android artifacts are uploaded as **papertrade-build**. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Implementation

- `MainActivity`: navigation, server refresh and connection state.
- `MarketScreens`, `AccountScreens`, `OrderTicket`: individual views and order review.
- `UiKit`: shared native Android view styling.
- `ApiClient`: asynchronous HTTP calls to the Spring Boot API.
- `ChartView`: Canvas charts generated deterministically; the range selector changes the illustration, not real historical data.
- `DemoBroker`: client-side snapshot model and fake test gateway. The app does not execute orders locally.
- `DemoStore`: display-only cache in private SharedPreferences; cached data cannot be traded while offline.
- `backend/`: Spring Boot REST API, JDBC repositories, transactional order service and Flyway SQL migration.

Seed: $24,750 cash plus 12 NVDA, 8 AAPL and 5 MSFT shares in PostgreSQL. The resulting total account value and unrealized P&L are computed from those holdings. All values are examples, not current market quotes. Clearing Android app storage does not reset the database.

### API contract

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/stocks` | Seeded simulated prices and names |
| GET | `/api/account` | Cash, positions and last 100 filled orders |
| POST | `/api/orders` | Submit an immediate paper buy/sell |

An order body is `{ "id": "<UUID>", "symbol": "NVDA", "buy": true, "quantity": 2 }`. Reusing the same ID and payload returns the existing fill; changing the payload for that ID returns HTTP 409. Insufficient funds or shares return HTTP 422 with a JSON `code` and `message`. All money values in API JSON are **integer cents**. The demo server binds to `127.0.0.1` and uses a single account without authentication.

## Verification

Backend integration tests use PostgreSQL to check balance/holding changes, rejected orders without mutation, duplicate requests and competing buys. Robolectric uses a fake gateway to check navigation, search/empty results, chart controls, activity recreation, order validation, confirmation and cached state; it exports screenshots for visual review.

Manual device checklist:
- Open every tab and stock, scroll to the bottom, try a large system font.
- Search by ticker and company, including an unmatched query.
- Toggle a favorite; restart and check it remains changed.
- Switch chart ranges and line/candle modes.
- With the server running, buy two shares, confirm, and check cash/holdings/order history. Restart the app and verify the state is fetched again.
- Stop the server: cached data should remain visible and new orders should be blocked. Restart it and tap Retry.
- Reject insufficient cash, overselling, blank and zero quantities.
- Cancel an order, rotate the device and verify there is no unintended fill.

## Scope and next steps

This is a demonstration system. It has no authentication, exchange matching, partial fills or real-time market feeds. Order execution is immediate at seeded prices. Before exposing it beyond a development machine, add account authentication and authorization, HTTPS, operational controls and a proper ledger.

Visual reference: [Webull trading platforms](https://www.webull.com/trading-platforms). No Webull logos, screenshots or proprietary assets are bundled.

