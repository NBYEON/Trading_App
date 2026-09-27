## PaperTrade: 가상 거래 시스템

자바로 제작된 안드로이드 클라이언트. Spring Boot API로 증권 갱신,  PostgreSQL로 계정 저장. 

## FlowChart

## 메인화면
<div>
<img width="567" height="1224" alt="Image" src="https://github.com/user-attachments/assets/89510192-7895-4704-b364-7f2bc1816ae8" />
<img width="563" height="1237" alt="Image" src="https://github.com/user-attachments/assets/7b77e385-2a17-44ef-9467-5cc54bb1d5f8" />
<img width="576" height="1238" alt="Image" src="https://github.com/user-attachments/assets/bafac4b1-b1dc-47c2-82e2-2252e1a69f91" />
<img width="574" height="1232" alt="Image" src="https://github.com/user-attachments/assets/26f355f4-3147-4689-86b2-93a803a65f77" />
</div>

## Open and run

1. Install Android Studio with Android SDK Platform 35. Install Docker Desktop to run both the API and PostgreSQL with one command, or install JDK 17, Maven and PostgreSQL 17 to run them locally.
2. From the repository root, run `docker compose up -d`. Docker starts PostgreSQL and then the API after the database is healthy. The first run downloads the Maven image and dependencies. To run the API outside Docker instead, run `docker compose up -d db` followed by `mvn -f backend/pom.xml spring-boot:run` in a separate terminal. For an existing PostgreSQL installation, create a `papertrade` database and user and set `DB_URL`, `DB_USER`, and `DB_PASSWORD` for the API.
3. Flyway creates the schema and one demo account. Verify `http://localhost:8080/api/stocks` in your browser. If startup fails, check `docker compose logs api --tail=80`.
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
