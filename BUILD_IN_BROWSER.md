# Getting FieldLog onto your phone — with only a browser

You do not need Android Studio. You do not need to install anything on your Chromebook.
GitHub will build the app on its own servers, for free, and hand you a file your phone
can install.

**What you need:** a browser, an Android phone, and a free GitHub account.

---

## Step 1 — Make a GitHub account

Go to **https://github.com** → Sign up. Free. Takes two minutes.

## Step 2 — Make a repository

Click the **+** in the top right → **New repository**.

- **Repository name:** `fieldlog`
- Leave it **Public** (public repos get unlimited free build time; private ones are
  capped at 2,000 minutes a month — plenty, but why bother)
- **Don't** tick "Add a README" — the project already has one
- Click **Create repository**

## Step 3 — Upload the code

Unzip `fieldlog.zip` on your Chromebook (in the Files app, right-click → Extract).

On the new empty repo page, click **uploading an existing file**.

Now drag the **contents** of the `fieldlog` folder into the browser — not the folder
itself. You want `app`, `gradle`, `.github`, `build.gradle.kts`, `settings.gradle.kts`,
`gradle.properties`, `README.md` all landing at the top level.

⚠️ **The `.github` folder is the important one and it's hidden.** In the ChromeOS Files
app press **Ctrl + .** to show hidden files first. If `.github` doesn't get uploaded,
nothing builds and you'll have no idea why.

Scroll down, click **Commit changes**.

## Step 4 — Watch it build

Click the **Actions** tab at the top of your repo.

You'll see a job running with a spinning yellow dot. It takes about 3–5 minutes.

- **Green tick ✅** → it built. Go to Step 5.
- **Red X ❌** → click into it, click "Build the app", and find the red error text.
  **Copy that error and paste it to me.** This is normal on a first build and it's a
  quick fix. It's also the first time a real compiler has ever seen this code.

## Step 5 — Install it on your phone

Once it's green, go to the **Releases** section on your repo's front page (right-hand
side). There'll be a release called **FieldLog build 1**.

**Now do this bit on your phone, not the Chromebook:**

1. Open your phone's browser, go to your repo (`github.com/YOURNAME/fieldlog`)
2. Tap **Releases** → the newest build
3. Tap **`app-debug.apk`** to download it
4. Tap the downloaded file
5. Android will say it can't install from unknown sources → tap **Settings** → allow
   your browser to install apps → go back and tap install

**FieldLog is now on your phone.** Open it. Add a job. Hit the amber slab.

---

## Making changes after this

Edit any file directly on GitHub (click the file → pencil icon → Commit changes).
Every commit triggers a new build automatically, and a new release appears a few
minutes later. Re-download, re-install.

For a proper editor in the browser: open your repo and **press the `.` key**. You get
a full VS Code editor, in the browser, free. Nothing to install.

---

## When you're ready for the Play Store

**Google Play Console is a website.** https://play.google.com/console — $25 one-time.
The whole submission process is done in a browser. Your Chromebook is fine for all of it.

The one extra thing you'll need is a **release build** signed with your own key (the
`.apk` above is a debug build — great for testing, not accepted by Play). When you get
to that point, say so and I'll add a second workflow that produces a signed `.aab` for
upload. It's the same idea: GitHub builds it, you download it, you upload it to Play.

---

## Why this is better than Android Studio anyway

- Nothing to install, nothing to update, no 8GB RAM requirement
- The build log is a permanent, shareable record — when something breaks, you can just
  paste it to me
- Same machine builds it every time, so "works on my computer" never happens
- It's how real teams ship apps
