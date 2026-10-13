# Telegram Group With Topics Setup

This guide walks you through setting up automatic artifact forwarding to your **Telegram Group with Forum Topics**.

---

## 1. Create Your Telegram Bot

1. Open Telegram and message [@BotFather](https://t.me/BotFather).
2. Send `/newbot` and follow the instructions to choose a display name and username (e.g. `SmartIslandBuildsBot`).
3. BotFather will provide an API Token:
   ```text
   123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ
   ```
   Save this token as your `TELEGRAM_BOT_TOKEN`.

---

## 2. Add the Bot to Your Telegram Group

1. Open your Telegram Group (with Topics enabled).
2. Go to **Group Info** -> **Administrators** -> **Add Administrator**.
3. Search for your bot and add it.
4. Ensure the bot has the following admin permissions:
   - ✅ **Post Messages** / **Send Messages**
   - ✅ **Manage Topics** (so it can post in forum topics)

---

## 3. How to Get Chat ID and Topic ID (in 5 Seconds)

In Telegram Desktop, Android, iOS, or Web:

1. Right-click or long-press on the topic where you want builds posted.
2. Select **Copy Link to Topic**.
3. Look at the copied link:
   ```text
   https://t.me/c/2345678901/42
   ```
   - **Chat ID:** `-1002345678901` *(the first number, prefixed with `-100`)*
     > Note: If you paste just `2345678901`, our forwarding script automatically prefixes `-100` for you.
   - **Topic ID:** `42` *(the number at the end)*

---

## 4. Recommended Multi-Topic Setup

Most communities prefer keeping official releases separate from dev test builds. You can set up:

| Channel / Topic | Purpose | Secret to Set |
|---|---|---|
| `#releases` or `#announcements` | Signed official release APKs | `TELEGRAM_RELEASE_TOPIC_ID` |
| `#dev-builds` or `#testing` | Nightly / snapshot debug APKs | `TELEGRAM_DEBUG_TOPIC_ID` |
| General (Single Topic) | All builds go to the same topic | `TELEGRAM_TOPIC_ID` |

---

## 5. Add GitHub Repository Secrets

In your GitHub repository, open:
```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

Add the following:

| Secret Name | Required? | Example | Description |
|---|---|---|---|
| `TELEGRAM_BOT_TOKEN` | **Yes** | `123456789:ABCdef...` | Token from @BotFather |
| `TELEGRAM_CHAT_ID` | **Yes** | `-1002345678901` | Your supergroup ID |
| `TELEGRAM_TOPIC_ID` | Optional | `1` | Fallback / default topic ID |
| `TELEGRAM_RELEASE_TOPIC_ID` | Optional | `10` | Specific topic ID for Release APKs |
| `TELEGRAM_DEBUG_TOPIC_ID` | Optional | `42` | Specific topic ID for Debug APKs |

---

## 6. Workflow Triggers

- **Push / Merge to `main` or Tag `v*`:**
  Builds and signs the release APK (`SmartIsland-v*.apk`), publishes the GitHub release, and forwards the APK to your Release Topic.
- **Push to `dev` or manual `workflow_dispatch`:**
  Builds `app-debug.apk` and forwards it to your Debug Topic.
- **Safe Fallback:**
  If the secrets are not set yet, the workflow logs an informational notice and completes without failing.
