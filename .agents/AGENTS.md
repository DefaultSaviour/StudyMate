# Testing and Verification Guidelines
When making code or UI changes, always verify them efficiently to avoid repeated errors and save tokens/usage.
- After implementing fixes, double-check that the code modifications actually replaced the intended logic (especially when dealing with hardware back button overrides or navigation).
- When a UI element is supposed to be removed, ensure that BOTH its XML definition and all associated programmatic references (like click listeners or visibility toggles) are fully stripped out to avoid duplicates or crashes.
- Do not blindly assume edits were fully successful—if multiple changes are made, review the file or build the app locally to confirm everything behaves as expected without wasting the user's time on partial fixes.
