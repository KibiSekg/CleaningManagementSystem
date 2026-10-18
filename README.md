# South Point CRMS (Android, XML layouts + Kotlin + Firebase)

Android conversion of the ASP.NET MVC project `KabeloSek/XISD6329_Task1_CRMS`.

* **UI:** XML layouts (`app/src/main/res/layout`) with ViewBinding and Material Components
* **Logic:** Kotlin Activities + one Firebase repository
* **Backend:** Firebase Authentication + Cloud Firestore (replaces SQL Server LocalDB)

Package `com.southpoint.crms` | minSdk 26 | compileSdk/targetSdk 34

## 1. Firebase setup (about 5 minutes)

1. https://console.firebase.google.com > create a project.
2. Project settings > **Add app > Android**, package name `com.southpoint.crms`.
3. Download `google-services.json` and **replace** the placeholder at `app/google-services.json`.
4. Build > **Authentication** > Get started > enable **Email/Password**.
5. Build > **Firestore Database** > Create database (production mode is fine).
6. Firestore > **Rules** tab > paste all of `firestore.rules` > **Publish**.

No Firebase Storage or paid plan is needed.

## 2. Run

Android Studio (Koala 2024.1+ recommended) > Open this folder > wait for Gradle sync > Run.

## 3. Screens: original view -> layout -> Activity

| Original view | XML layout | Kotlin Activity |
|---|---|---|
| (app start) | activity_splash | ui/SplashActivity |
| Register | activity_register | ui/auth/RegisterActivity |
| RegisterCleaner | activity_register_cleaner | ui/auth/RegisterCleanerActivity |
| Login / LoginCleaner | activity_login (shared) | ui/auth/LoginActivity |
| StudentHome | activity_student_home | ui/student/StudentHomeActivity |
| WhoCleaningIsFor | activity_who_for | ui/student/WhoForActivity |
| RequestCleaningForm | activity_request_me | ui/student/RequestForMeActivity |
| RequestForElse | activity_request_else | ui/student/RequestForElseActivity |
| MyBookings | activity_my_bookings + item_booking | ui/student/MyBookingsActivity |
| StudentNotifications | activity_notifications + item_notification | ui/student/NotificationsActivity |
| StudentHelpSupport | activity_help_support | ui/student/HelpSupportActivity |
| CleaningGuidelines | activity_guidelines | ui/student/GuidelinesActivity |
| ContactResidenceManagement | activity_contact | ui/student/ContactActivity |
| ReportProblem | activity_report_problem | ui/student/ReportProblemActivity |
| StudentProfile / CleanerProfile | activity_profile (shared) | ui/ProfileActivity |
| CleanerHome | activity_cleaner_home | ui/cleaner/CleanerHomeActivity |
| CleanerRequests | activity_cleaner_requests + item_request | ui/cleaner/CleanerRequestsActivity |
| CleanerBookings | activity_cleaner_bookings + item_job + item_booking | ui/cleaner/CleanerBookingsActivity |

## 4. Where the inner workings live

* `data/CrmsRepository.kt` - every Firebase call (register, login, create/accept/complete booking, live listeners, profile update)
* `data/Models.kt` - UserProfile, Booking, AppNotification, dropdown options
* `data/SessionManager.kt` - who is signed in (replaces the ASP.NET session)
* `ui/BaseActivity.kt`, `ui/UiExtensions.kt` - shared helpers (toolbar, date picker, validation)

## 5. Firestore layout

```
users/{uid}                 role, name, email, room, externalBookingId
externalIds/{externalId}    uid
bookings/{bookingId}        studentId, roomNumber, bookingDate, roomType, timeSlot, cleaningType,
                            specialInstructions, status, cleanerId, cleanerName, requestedBy, createdAt
bookingSecrets/{bookingId}  studentId, passkey   (never readable by the app; checked by the rules)
notifications/{id}          studentId, message, type, bookingId, cleanerName, passkey, bookingDate, createdAt
problemReports/{id}         studentId, issue, room, photoAttached, createdAt
```

## 6. Intentional differences from the web app

* Passwords are handled by Firebase Auth (hashed); the old app stored plain text and built SQL by string concatenation.
* The profile page can't *show* your password (Firebase can't return it). You type your current password to confirm an email/password change. A new email only takes effect after you click the verification link Firebase sends.
* The passkey is verified by the Firestore rules, so cleaners can never read it.
* Report a Problem saves the text/room; the photo is required but not uploaded (Firebase Storage needs a paid plan).
* The Help & Support card was commented out on the original home page; it is enabled here.
* Room dropdowns are one flat list (101-510) instead of grouped by floor.
* Lists update live. Screens are locked to portrait.
