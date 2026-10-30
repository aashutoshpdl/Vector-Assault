# Autonomous Dev Engine 🤖

A fully autonomous, multi-agent development engine running on **GitHub Actions** and powered by **Google Gemini 2.0 Flash**.

You (the Client) create a GitHub Issue on your phone → **Manager Agent** plans the architecture and selects the optimal programming language → **Developer Agent** writes the full multi-file code and opens a Pull Request → **Universal CI/CD** publishes live web previews or downloadable APKs/binaries.

---

## 🌟 Universal Language & Platform Support

The agent is **not** restricted to any single tech stack. The AI analyzes your requirements and automatically selects:
* 🌐 **Web Applications (HTML/CSS/JS, React, Next.js):** Automatically deployed to **GitHub Pages** for instant preview.
* 📱 **Mobile Applications (Flutter / Android):** Compiled into a `.apk` file attached to **GitHub Releases** for direct phone download.
* ⚡ **Systems & CLI (Rust, Go, C++):** Compiled into release binaries.
* 🐍 **Python / ML / Automation:** Bundled with dependencies and execution scripts.

---

## 🚀 Quick Setup (Push to GitHub)

### 1. Push this folder to a new GitHub Repository
Open your terminal inside this directory and run:

```bash
cd /Users/macbookpro/Documents/project/agents/autonomous-dev-engine
git init
git add .
git commit -m "Initial commit: Autonomous Dev Engine"
git branch -M main

# Create the repo on GitHub (replace with your repo name):
gh repo create my-autonomous-project --public --source=. --remote=origin --push
```

### 2. Add Your Gemini API Key
1. Go to your new repo on GitHub.
2. Navigate to: **Settings** → **Secrets and variables** → **Actions**.
3. Click **New repository secret**:
   * **Name:** `GEMINI_API_KEY`
   * **Value:** *(Paste your Google AI Gemini API key)*
4. Click **Add secret**.

### 3. Enable GitHub Pages (For Web Previews)
1. Go to: **Settings** → **Pages**.
2. Under **Build and deployment > Source**, select: **GitHub Actions**.

---

## 📱 How You Use It (The Daily Routine)

1. Open the GitHub mobile app on your phone.
2. Go to **Issues** → **New Issue**.
3. Type your instructions (they can be vague or feature-packed):
   * *Example 1 (Mobile):* "Create a mobile habit tracker app with dark mode, streak counter, and notification reminders."
   * *Example 2 (Web):* "Build an interactive audio visualizer with modern neon aesthetics."
   * *Example 3 (Rust):* "High-speed CLI tool that scans a directory and finds duplicate images by hash."
4. Click **Submit**.

### What Happens Automatically:
1. **Manager Agent comments** on your issue with the chosen tech stack and architectural plan.
2. **Developer Agent generates the code**, opens a branch, and submits a **Pull Request**.
3. **You review and tap "Merge"** on your phone.
4. **Universal Release Pipeline runs:**
   * If it's a web app: Deploys to `https://<username>.github.io/<repo-name>/`.
   * If it's a mobile/CLI app: Publishes a **GitHub Release** with the `.apk` or binary ready to download.
