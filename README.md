# FieldLog

An offline time-and-expense logger for people who work where there's no signal.

Clock in on a job, clock out, log what you spent, export a CSV. No account, no
sign-up, no server, no ads. **The app has no internet permission at all** — Android
will not let it open a network connection even if it wanted to. That's the whole pitch.

---

## What's in the box

| Screen | What it does |
|---|---|
| **Clock** | One giant button. Amber = clock in. Green = a live running timer, plus what you've earned so far today. |
| **Jobs** | Your jobs/clients, each with an optional hourly rate. |
| **Money** | Log fuel, materials, parking. Tag it to a job, mark it billable or not. |
| **Totals** | Hours, earned, spent, per job — this week / this month / all time. Export to CSV. |

Everything is stored in a SQLite database file on the phone. Airplane mode changes nothing.

---

## Part 1 — Get it running on your own phone

You have never coded before. That's fine. This part is all clicking.

### 1. Install Android Studio

Download it from **https://developer.android.com/studio** and install it. It's a big
download (~1GB) and the first launch downloads more. Accept every default.

When it asks about the "Android SDK", say yes to everything. That's the toolbox
that turns this code into an app.

### 2. Open this project

Unzip the `fieldlog` folder somewhere you'll find it again (Documents is fine).

In Android Studio: **File → Open** → pick the `fieldlog` folder itself (the one with
`settings.gradle.kts` inside it). Not a folder above it, not a folder below it.

Now wait. The bottom of the window will say "Gradle sync" and churn for 2–10 minutes
the first time — it's downloading the libraries the app needs. Let it finish.
**You need internet for this step only.** The app itself never uses it.

### 3. Turn on Developer Mode on your phone

On your Android phone:

1. **Settings → About phone**
2. Find **Build number**. Tap it **7 times**. It'll say "You are now a developer."
3. Go back to **Settings → System → Developer options**
4. Turn on **USB debugging**

### 4. Run it

1. Plug the phone into the computer with a USB cable.
2. On the phone, a popup asks "Allow USB debugging?" → **Allow**.
3. In Android Studio, top toolbar: your phone's name should appear in the dropdown.
4. Hit the green **▶ Run** button.

First build takes a few minutes. Then FieldLog appears on your phone.

**If the phone doesn't show up:** it's almost always the cable. Many cheap USB cables
are charge-only and carry no data. Try a different one.

---

## Part 2 — Publish it to Google Play

### 1. Make a signing key

This is a password-protected file that proves the app is yours. **If you lose it, you
can never update your own app again.** Back it up somewhere safe, twice.

In Android Studio: **Build → Generate Signed App Bundle / APK → Android App Bundle → Next
→ Create new...**

Fill it in, pick a strong password, write the password down somewhere real.

### 2. Build the release bundle

Same menu, choose your key, pick **release**, hit Finish.

Android Studio spits out an `.aab` file. It'll show a popup with a "locate" link —
click that to find it. (It lives in `app/release/`.)

### 3. Play Console

1. Go to **https://play.google.com/console** and pay the **$25 one-time** developer fee.
2. **Create app** → name it, pick Free.
3. Work through the checklist Google gives you. The tedious but required bits:
   - **Privacy policy.** You need a URL even though you collect nothing. Write one
     page saying "This app collects no data and has no internet access," host it free
     on GitHub Pages or Google Sites, paste the link.
   - **Data safety form.** Tick **"No data collected."** For once this is true, and
     it's a genuine selling point — it shows on your store listing.
   - **Content rating.** Answer honestly; you'll get "Everyone."
   - **Screenshots.** Take them on your phone (power + volume-down). You need at
     least 2. A phone screenshot of the green running clock is your best one.
4. Upload the `.aab` under **Production → Create release**.
5. Submit. First review usually takes a few days to a couple of weeks.

---

## If you want to change something

The code is commented. The files worth knowing:

```
app/src/main/java/com/fieldlog/app/
├── data/
│   ├── Model.kt        <- What a Job / TimeEntry / Expense IS
│   ├── Dao.kt          <- The database questions
│   ├── Database.kt     <- Creates the database file
│   └── Repository.kt   <- The RULES (e.g. one clock running at a time)
├── ui/
│   ├── theme/Theme.kt  <- Colours, fonts, sizes. Change the palette here.
│   ├── AppScaffold.kt  <- The four tabs
│   └── screens/        <- One file per screen
├── util/Format.kt      <- Money and time formatting
└── export/CsvExport.kt <- The CSV writer
```

**Common first tweaks:**

- **Rename the app:** `app/src/main/res/values/strings.xml`, change `app_name`.
- **Change the colours:** `ui/theme/Theme.kt`, top of the file.
- **Add an expense category:** `data/Model.kt`, the `EXPENSE_CATEGORIES` list.

⚠️ **One rule:** if you change anything in `data/Model.kt` (add a field, rename one),
you must bump `version = 1` to `version = 2` in `Database.kt` and add a migration —
otherwise the app will crash for anyone who already has data. Ask before you do that
one; it's the single easiest way to break the app for real users.

---

## Money: the plan

**The app is free. There is no billing code and no INTERNET permission.** That is
deliberate, and it's the current strategy — ship free, find out if anyone actually
wants this, charge later if they do.

### What's already in place for later

`data/Entitlement.kt` does nothing today, but it does two things that are impossible
to add retroactively:

1. **It stamps the install date on first launch.** The day you add a paywall, you'll
   want to grandfather everyone already using the app — free forever, founding user.
   You cannot backfill an install date. If you don't record it from the first public
   release, that information is gone and you'll have to choose between charging your
   earliest supporters or giving the app away to everyone.

2. **It's the single place features get gated.** Every paywall-able action already
   asks `Entitlement` for permission and always gets a yes. When you add billing,
   you change `isPro()` and nothing else in the app changes.

### Adding billing later (when you have real users)

1. Add the Play Billing library to `app/build.gradle.kts`.
2. Add `<uses-permission android:name="android.permission.INTERNET" />` to the manifest.
   ⚠️ **This is the moment you lose the "cannot connect to the internet" claim.** You
   still have no server and still collect no data, so "No data collected" on the data
   safety form stays true — but the absolute version of the pitch is spent. Spend it
   on purpose.
3. In `Entitlement.kt`, set `FOUNDING_CUTOFF` to **that day's date in epoch millis**.
   Everyone already using the app stays free automatically.
4. Change `isPro()` to `isFoundingUser(context) || hasActivePurchase(context)`.
5. Set the free-tier limits you want (`maxJobs`, `canExportCsv` are already wired).

That's the whole job. No screens change.

### What NOT to do

- **Don't charge $1/year.** Google takes 15%, so you net $0.85 per person per year —
  and you'll still handle renewals, failed cards, and refund emails. You'd answer a
  20-minute support email over 85 cents. A subscription needs an ongoing cost to
  recur against (a server, syncing, backups). This app has none.
- A one-time unlock (~$4.99) fits an app with no server far better. 1,000 one-time
  buyers beats 1,000 annual subscribers by 5× in year one, with none of the machinery.
- If you ever want real recurring revenue, add something worth recurring on —
  cloud backup, multi-device, team timesheets — and charge properly for *that*.

---

## What to actually do next

Not "publish." **Validate.**

You have no analytics — no internet, remember — so you cannot learn from a dashboard.
You have to learn from people. That's a feature, not a limitation: it forces the only
research that's worth anything.

1. Get it running on your own phone (Part 1 above).
2. **Use it yourself for a week**, on something real.
3. Put it in front of **5 people who actually work in the field.** Hand them the phone,
   say nothing, and watch. Do not explain. Do not help. The places they hesitate are
   your bug list.
4. Ship it free to Play. Get to ~50 real users.
5. Ask the ones who stuck around: *what would you pay for?* Then price that.

Charging before step 5 is guessing.

---

## Honest notes

- **This has not been run on a real device yet.** The code was written carefully and
  the money/CSV logic was tested, but the first build may surface a version mismatch
  or a missing import. Android Studio will underline anything wrong in red and usually
  offers a one-click fix. Paste any error you get and it can be sorted.
- **Your competition is a paper notebook**, not another app. Beat the notebook.
