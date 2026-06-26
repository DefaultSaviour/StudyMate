# Testing and Verification Guidelines
When making code or UI changes, always verify them efficiently to avoid repeated errors and save tokens/usage.
- After implementing fixes, double-check that the code modifications actually replaced the intended logic (especially when dealing with hardware back button overrides or navigation).
- When a UI element is supposed to be removed, ensure that BOTH its XML definition and all associated programmatic references (like click listeners or visibility toggles) are fully stripped out to avoid duplicates or crashes.
- Do not blindly assume edits were fully successful—if multiple changes are made, review the file or build the app locally to confirm everything behaves as expected without wasting the user's time on partial fixes.

# UI Failure Protocol
- If a UI fix attempt fails and the user points out that it is still broken after I thought I fixed it, I MUST run a UI test myself to verify the issue after my next fix attempt.
- Alternatively, I should explicitly ask the user if they want me to run a UI test after the second fix attempt.

# Project Documentation and Context
- This repository contains a highly detailed documentation file at `CLAUDE.md`. 
- **CRITICAL**: Before making architectural changes, adding new features, or modifying database schemas, you MUST read `CLAUDE.md` to understand the project's existing conventions, MVVM structure, and database history.
- Do not guess the architecture or authentication flows; they are extensively documented there.
