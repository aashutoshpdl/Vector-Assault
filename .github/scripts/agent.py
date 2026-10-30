import os
import sys
import json
import subprocess
import glob
from google import genai
from google.genai import types

# Force unbuffered output so logs appear in real-time on GitHub Actions
sys.stdout.reconfigure(line_buffering=True)

api_key = os.environ.get("GEMINI_API_KEY")
if not api_key:
    raise ValueError("GEMINI_API_KEY environment variable is missing.")

gh_token = os.environ.get("GH_TOKEN", "")
repo = os.environ.get("REPO", "")
issue_num = os.environ.get("ISSUE_NUMBER", "1")
issue_title = os.environ.get("ISSUE_TITLE", "New Feature Request")
issue_body = os.environ.get("ISSUE_BODY", "")
model_name = os.environ.get("GEMINI_MODEL", "gemini-3.8-flash")

# Configure Git authenticated remote so git push never hangs for credentials
if gh_token and repo:
    subprocess.run(
        ["git", "remote", "set-url", "origin", f"https://x-access-token:{gh_token}@github.com/{repo}.git"],
        check=False
    )

client = genai.Client(api_key=api_key)

print(f"🚀 Processing Issue #{issue_num}: {issue_title} using model {model_name}")

# ---------------------------------------------------------------------------
# PHASE 0: SCAN EXISTING REPOSITORY CONTEXT
# Reads existing source files so the agent builds ON TOP of existing code.
# ---------------------------------------------------------------------------
print("🔍 Scanning existing codebase...")
existing_files = {}
ignored_dirs = {".git", ".github", "node_modules", "dist", "build", ".venv"}
allowed_extensions = {".html", ".css", ".js", ".jsx", ".ts", ".tsx", ".json", ".rs", ".py", ".go", ".sh", ".md"}

for root, dirs, files in os.walk("."):
    dirs[:] = [d for d in dirs if d not in ignored_dirs]
    for file in files:
        ext = os.path.splitext(file)[1]
        if ext in allowed_extensions:
            path = os.path.normpath(os.path.join(root, file))
            try:
                with open(path, "r", encoding="utf-8") as f:
                    content = f.read()
                    # Only include files under 100KB to stay fast
                    if len(content) < 100000:
                        existing_files[path] = content
            except Exception:
                pass

print(f"📁 Found {len(existing_files)} existing project files in repository.")
existing_code_summary = ""
if existing_files:
    existing_code_summary = "### EXISTING CODEBASE FILES IN REPOSITORY:\n"
    for path, content in existing_files.items():
        existing_code_summary += f"\n--- FILE: {path} ---\n{content}\n"

# ---------------------------------------------------------------------------
# PHASE 1: MANAGER AGENT
# Selects the best language, tech stack, and project architecture.
# ---------------------------------------------------------------------------
print("🧠 Manager Agent: Determining optimal architecture and tech stack...")

manager_system_instruction = """
You are an elite Chief Technology Officer & Technical Product Manager.
Analyze the user's issue request in the context of any existing codebase:
1. If the repository already has code, plan how to EXTEND, REFACTOR, or ADD FEATURES to the existing code.
2. If this is a new project, select the optimal language and framework.
3. Outline the essential files to create or modify.

Output strictly valid JSON with this schema:
{
  "project_type": "<web | mobile | cli | data | desktop>",
  "stack_chosen": "<e.g., React / Tailwind / Rust / Python>",
  "summary": "<2-3 sentence executive summary of the plan>",
  "plan_steps": ["step 1", "step 2", "step 3"]
}
"""

manager_prompt = f"""
Issue Title: {issue_title}
Issue Description:
{issue_body}

{existing_code_summary}
"""

manager_res = client.models.generate_content(
    model=model_name,
    contents=manager_prompt,
    config=types.GenerateContentConfig(
        system_instruction=manager_system_instruction,
        response_mime_type="application/json"
    )
)

plan = json.loads(manager_res.text)

# Post Manager's architectural decision to the issue
plan_bullets = "\n".join([f"- {s}" for s in plan.get("plan_steps", [])])
comment_body = (
    f"### 🏛️ Manager Agent Plan\n\n"
    f"**Selected Stack:** `{plan.get('stack_chosen', 'Auto-selected')}` ({plan.get('project_type', 'General')})\n\n"
    f"**Architecture Summary:**\n{plan.get('summary', '')}\n\n"
    f"**Roadmap:**\n{plan_bullets}\n\n"
    f"🔨 *Developer Agent is now building/updating code...*"
)

subprocess.run(["gh", "issue", "comment", str(issue_num), "--repo", repo, "--body", comment_body], check=False)

# ---------------------------------------------------------------------------
# PHASE 2: DEVELOPER AGENT
# Generates or updates code, building directly on top of existing files.
# ---------------------------------------------------------------------------
print("💻 Developer Agent: Writing source code and build pipelines...")

branch_name = f"feature/issue-{issue_num}"
subprocess.run(["git", "checkout", "-B", branch_name], check=False)

dev_system_instruction = """
You are a Staff Software Engineer.
Your task is to implement the requested feature or fix.

CRITICAL INSTRUCTIONS:
1. If existing files are provided, BUILD DIRECTLY ON TOP OF THEM. Do not break existing features; integrate the new requirements cleanly.
2. If this is net-new code, provide the complete, functional application.
3. Always include or update `build.sh` to package/build the outputs into `./dist`.
4. Output MUST be strictly valid JSON mapping relative file paths to their full string contents:
{
  "index.html": "<full updated code>",
  "app.js": "<full updated code>",
  "build.sh": "#!/usr/bin/env bash\\n..."
}
"""

dev_prompt = f"""
Implement this task:
Title: {issue_title}
Requirements: {issue_body}
Architecture Plan: {plan.get('summary')}

{existing_code_summary}
"""

print("⏳ Calling Gemini Developer Agent (generating updated source files)...")
dev_res = client.models.generate_content(
    model=model_name,
    contents=dev_prompt,
    config=types.GenerateContentConfig(
        system_instruction=dev_system_instruction,
        response_mime_type="application/json"
    )
)

print("📦 Decoding and writing generated files...")
code_files = json.loads(dev_res.text)

for file_path, content in code_files.items():
    clean_path = os.path.normpath(file_path.lstrip("/"))
    if os.path.dirname(clean_path):
        os.makedirs(os.path.dirname(clean_path), exist_ok=True)
    
    with open(clean_path, "w", encoding="utf-8") as f:
        f.write(content)
    
    if clean_path.endswith(".sh"):
        os.chmod(clean_path, 0o755)
    print(f"  ✓ Updated/Created: {clean_path}")

# ---------------------------------------------------------------------------
# PHASE 3: COMMIT & OPEN PULL REQUEST
# ---------------------------------------------------------------------------
print("🚀 Committing changes and creating Pull Request...")

subprocess.run(["git", "config", "user.name", "Gemini Developer Agent"], check=False)
subprocess.run(["git", "config", "user.email", "agent@github.actions"], check=False)
subprocess.run(["git", "add", "."], check=False)

commit_msg = f"feat: {issue_title} (closes #{issue_num})\n\nStack: {plan.get('stack_chosen')}"
subprocess.run(["git", "commit", "-m", commit_msg], check=False)

print(f"⬆️ Pushing branch {branch_name} to origin...")
subprocess.run(["git", "push", "-u", "origin", branch_name, "--force"], check=False)

pr_body = f"""
## 🤖 Automated Solution for #{issue_num}

### 🛠️ Architecture & Stack
* **Language & Framework:** `{plan.get('stack_chosen')}`
* **Category:** `{plan.get('project_type')}`

### 📝 Summary
{plan.get('summary')}

### 📦 Modified / Created Files:
{', '.join([f'`{k}`' for k in list(code_files.keys())])}

---
*Once merged into `main`, GitHub Actions will automatically trigger the universal build pipeline to publish live web previews or downloadable release binaries/APKs.*

Closes #{issue_num}
"""

print("📬 Creating GitHub Pull Request...")
pr_create_res = subprocess.run([
    "gh", "pr", "create",
    "--repo", repo,
    "--title", f"Feature: {issue_title}",
    "--body", pr_body,
    "--head", branch_name,
    "--base", "main"
], capture_output=True, text=True, check=False)

pr_url = pr_create_res.stdout.strip()
if not pr_url:
    print(f"⚠️ PR creation note: {pr_create_res.stderr.strip()}")
    pr_url = f"https://github.com/{repo}/pull/new/{branch_name}"
else:
    print(f"✅ PR opened: {pr_url}")

# Post completion notification directly on the issue
completion_comment = f"✅ **Developer Agent has completed the implementation!**\n\nPull Request is ready for your review: {pr_url}\n\nMerge it into `main` to trigger the live preview."
subprocess.run(["gh", "issue", "comment", str(issue_num), "--repo", repo, "--body", completion_comment], check=False)

print("🎉 Everything finished successfully!")
