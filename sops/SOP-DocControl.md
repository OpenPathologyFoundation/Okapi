# Standard Operating Procedure: Git Contribution Workflow

**Objective:** To standardize the process of submitting code changes, ensuring clean history and proper peer review.
**Scope:** All contributors submitting changes to the repository.

---

## Phase 1: Local Development
*Execute these steps in your terminal.*

### 1. Synchronization
Always start with the latest version of the codebase to avoid conflicts.

```bash
git checkout main
git pull origin main
```

### 2. Branch Creation

**Rule:** Never commit directly to `main`.
Create a feature branch with a descriptive name using the convention `type/description`.

* Types: `feature`, `fix`, `docs`, `refactor`
* Example: `feature/user-login`, `fix/header-alignment`

```bash
git checkout -b feature/your-branch-name
```

### 3. Staging and Committing

Stage your changes and create a snapshot.

```bash
# Stage specific files (Recommended)
git add path/to/file.js

# OR Stage all changes (Use with caution - check git status first)

git status
git add . 
#Or better
git add -A
# That includes deleted files.

# Commit with a descriptive message
git commit -m "Brief summary of changes (e.g., Add JWT authentication logic)"
```

### 4. Push to Remote

Make sure you are on the correct branch before pushing.
Push your branch to the remote server. The `-u` flag establishes tracking.

```bash
git push -u origin feature/your-branch-name
```

---

## Phase 2: Pull Request (PR)

*Execute these steps in your web browser (GitHub/GitLab).*

### 1. Open Pull Request

Navigate to the repository. You should see a "Compare & pull request" prompt. If not, go to the **Pull Requests** tab and select your branch.

### 2. PR Description Standard

Fill out the PR description using the following template to ensure clarity.

> **Type:** [Feature / Bugfix / Documentation]
> **Description:**
> * **What:** Concise summary of the changes (e.g., Created new Login component).
> * **Why:** The business or technical reason for the change (e.g., To allow user access).
> * **Testing:** Steps taken to verify the change (e.g., Tested locally on Chrome).
> 
> 
> **Ticket ID:** [JIRA-123 / Issue #45] (if applicable)

### 3. Review Process

* Assign a reviewer if required.
* Address any comments or requested changes.
* Once approved, merge the Pull Request.

---

## Phase 3: Post-Merge Cleanup

*Execute these steps in your terminal after the PR is merged.*

Return to the local environment, update the main branch, and remove the obsolete feature branch.

```bash
# 1. Switch to main
git checkout main

# 2. Pull the new changes (including your merged work)
git pull origin main

# 3. Delete the local feature branch
git branch -d feature/your-branch-name
```