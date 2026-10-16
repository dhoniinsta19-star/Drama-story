# Drama Story Android starter — phone-only setup

## Included
- Home and published episode list (Firestore)
- Email/password login and sign-up (Firebase Authentication)
- Video playback from HTTPS URLs
- Episode download through Android DownloadManager
- Google Mobile Ads test banner
- Admin screen placeholder (secure upload is intentionally not enabled)

## Build from an Android phone using GitHub Actions
1. Create a GitHub repository in Chrome on your phone.
2. Upload the contents of this ZIP while preserving folder structure.
3. In Firebase Console, create a project and add an Android app with package name `com.example.dramastory`.
4. Download `google-services.json` and upload it into the repository's `app/` folder.
5. In Firebase Authentication, enable Email/Password.
6. Create Firestore collection `episodes`. Each document should have:
   - `title`: string
   - `description`: string
   - `videoUrl`: string (HTTPS video URL you are licensed to distribute)
   - `thumbnailUrl`: string (optional)
   - `published`: boolean (`true` to show it)
7. Add the workflow at `.github/workflows/android.yml` (already included).
8. Open the repository's Actions tab and run **Build Drama Story APK**. Download the `drama-story-debug-apk` artifact from the completed workflow.
9. Install the APK on your phone. Android may ask to permit installation from your browser/file manager.

## Important
- Firebase setup and correct security rules are required before the app will work.
- Configure Firestore rules so users cannot change episode data unless authorized.
- The Admin screen is a placeholder. Implement uploads using a trusted backend/Cloud Function that verifies Firebase admin custom claims. Never rely on a client-side button to protect uploads.
- AdMob IDs in this project are Google's test IDs. Replace them with your registered app/ad unit IDs before release and follow ad consent and platform policies.
- Do not distribute copyrighted animation, anime, or drama videos without a licence. Downloads should only be enabled where the licence permits offline copies.
- This is a starter project, not a finished production app. Test and secure it before public release.
