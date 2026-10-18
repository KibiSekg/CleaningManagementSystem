package com.southpoint.crms.data

import android.app.Activity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

/** Thrown when a student picks a room that another student already registered. */
class RoomTakenException(room: String) : Exception("Room $room is already taken. Please choose another room.")

/**
 * Single place that talks to Firebase (Authentication + Realtime Database).
 *
 * Realtime Database layout
 *   users/{uid}                          role, name, email, room, externalBookingId
 *   externalIds/{externalId}             uid
 *   rooms/{room}                         true            (a room is "taken" when this exists)
 *   bookings/{bookingId}                 studentId, requestedBy, roomNumber, bookingDate, roomType, timeSlot,
 *                                        cleaningType, specialInstructions, status, cleanerId, cleanerName,
 *                                        createdAt, enteredPasskey
 *   bookingSecrets/{bookingId}           studentId, passkey      (never readable by the app)
 *   notifications/{studentId}/{id}       message, type, bookingId, cleanerName, passkey, bookingDate, createdAt
 *   problemReports/{id}                  studentId, issue, room, photoAttached, createdAt
 */
object CrmsRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase get() = FirebaseConfig.database()

    private fun ref(path: String) = db.getReference(path)

    val currentUid: String? get() = auth.currentUser?.uid
    val currentEmail: String? get() = auth.currentUser?.email
    val currentDisplayName: String? get() = auth.currentUser?.displayName

    // ---------------------------------------------------------------- rooms

    /** Room numbers that already belong to a registered student. Readable before signing in. */
    suspend fun loadTakenRooms(): Set<String> {
        val snap = ref("rooms").get().await()
        return snap.children.mapNotNull { it.key }.toSet()
    }

    suspend fun isRoomTaken(room: String): Boolean = ref("rooms/$room").get().await().exists()

    // ---------------------------------------------------------------- auth

    suspend fun registerStudent(room: String, name: String, email: String, password: String) {
        val cleanEmail = email.trim()
        if (isRoomTaken(room)) throw RoomTakenException(room)

        val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Account could not be created.")
        try {
            createStudentProfile(uid, cleanEmail, name, room)
        } catch (e: Exception) {
            runCatching { result.user?.delete()?.await() } // don't leave a half-created account
            throw e
        }
        auth.signOut() // the student logs in on the next screen
    }

    suspend fun registerCleaner(name: String, email: String, password: String) {
        val cleanEmail = email.trim()
        val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Account could not be created.")
        try {
            createCleanerProfile(uid, cleanEmail, name)
        } catch (e: Exception) {
            runCatching { result.user?.delete()?.await() }
            throw e
        }
        auth.signOut()
    }

    /**
     * Saves the student's profile and claims their room in ONE atomic write: if somebody else claimed the
     * room a moment earlier, nothing is saved and RoomTakenException is thrown.
     */
    suspend fun createStudentProfile(uid: String, email: String, name: String, room: String): UserProfile {
        val externalId = "EB" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).uppercase()
        val cleanName = name.trim()

        val updates = mapOf<String, Any?>(
            "users/$uid" to mapOf(
                "role" to Roles.STUDENT,
                "name" to cleanName,
                "email" to email,
                "room" to room,
                "externalBookingId" to externalId
            ),
            "externalIds/$externalId" to uid,
            "rooms/$room" to true
        )
        try {
            db.reference.updateChildren(updates).await()
        } catch (e: Exception) {
            if (runCatching { isRoomTaken(room) }.getOrDefault(false)) throw RoomTakenException(room)
            throw e
        }
        return UserProfile(uid, Roles.STUDENT, cleanName, email, room, externalId)
    }

    suspend fun createCleanerProfile(uid: String, email: String, name: String): UserProfile {
        val cleanName = name.trim()
        ref("users/$uid").setValue(
            mapOf("role" to Roles.CLEANER, "name" to cleanName, "email" to email)
        ).await()
        return UserProfile(uid, Roles.CLEANER, cleanName, email)
    }

    /** Signs in and makes sure the account really is the expected role. */
    suspend fun login(email: String, password: String, expectedRole: String): UserProfile {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Login failed.")
        val profile = loadProfile(uid)
        if (profile == null || profile.role != expectedRole) {
            auth.signOut()
            throw IllegalStateException(
                if (expectedRole == Roles.STUDENT) "This is not a student account. Use the cleaner login."
                else "This is not a cleaner account. Use the student login."
            )
        }
        return profile
    }

    /**
     * Google single sign-on (opens the Google account picker in a browser tab).
     * Returns the saved profile, or null for a brand-new Google user who still has to finish setting up
     * (choose a room / confirm their name).
     */
    suspend fun signInWithGoogle(activity: Activity): UserProfile? {
        val provider = OAuthProvider.newBuilder("google.com")
            .addCustomParameter("prompt", "select_account")
            .build()
        val result = auth.startActivityForSignInWithProvider(activity, provider).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Google sign-in failed.")
        return loadProfile(uid)
    }

    suspend fun loadProfile(uid: String): UserProfile? {
        val snap = ref("users/$uid").get().await()
        if (!snap.exists()) return null
        return UserProfile(
            uid = uid,
            role = snap.str("role").orEmpty(),
            name = snap.str("name").orEmpty(),
            email = auth.currentUser?.email ?: snap.str("email").orEmpty(),
            room = snap.str("room").orEmpty(),
            externalBookingId = snap.str("externalBookingId").orEmpty()
        )
    }

    fun logout() = auth.signOut()

    /**
     * Profile screen: change email and/or password.
     * Firebase requires the current password (re-authentication) for both.
     * Email changes need a verification link, so the address only changes once it is confirmed.
     */
    suspend fun updateCredentials(currentPassword: String, newEmail: String, newPassword: String): String {
        val user = auth.currentUser ?: throw IllegalStateException("You are not signed in.")
        val oldEmail = user.email ?: throw IllegalStateException("Your account has no email address.")

        try {
            user.reauthenticate(EmailAuthProvider.getCredential(oldEmail, currentPassword)).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw IllegalStateException("Your current password is incorrect.")
        }

        val parts = mutableListOf<String>()
        val cleanNewEmail = newEmail.trim()
        if (cleanNewEmail.isNotEmpty() && !cleanNewEmail.equals(oldEmail, ignoreCase = true)) {
            user.verifyBeforeUpdateEmail(cleanNewEmail).await()
            parts += "A verification link was sent to $cleanNewEmail. Your email changes once you confirm it."
        }
        if (newPassword.isNotBlank()) {
            user.updatePassword(newPassword).await()
            parts += "Password updated."
        }
        return if (parts.isEmpty()) "Nothing to update." else parts.joinToString(" ")
    }

    // ------------------------------------------------------------ bookings

    /**
     * Creates a Pending booking + its secret passkey + a "Requested" notification in one atomic write.
     * targetStudentId is the student the booking belongs to (yourself, or the student found
     * through their External Booking ID).
     */
    suspend fun createBooking(
        targetStudentId: String,
        onBehalfOfSomeoneElse: Boolean,
        room: String,
        date: String,
        roomType: String,
        timeSlot: String,
        cleaningType: String,
        instructions: String
    ) {
        val requester = currentUid ?: throw IllegalStateException("You are not signed in.")
        val bookingId = ref("bookings").push().key ?: throw IllegalStateException("Could not create booking.")
        val notificationId = ref("notifications/$targetStudentId").push().key
            ?: throw IllegalStateException("Could not create booking.")
        val passkey = (100000 + SecureRandom().nextInt(900000)).toString()
        val now = System.currentTimeMillis()

        val booking = Booking(
            id = bookingId,
            studentId = targetStudentId,
            roomNumber = room,
            bookingDate = date,
            roomType = roomType,
            timeSlot = timeSlot,
            cleaningType = cleaningType,
            specialInstructions = instructions.trim(),
            status = Status.PENDING,
            cleanerId = null,
            cleanerName = null,
            requestedBy = requester,
            createdAt = now
        )

        val message = if (onBehalfOfSomeoneElse)
            "A cleaning request has been submitted on your behalf. Give this passkey to your cleaner once the job is done."
        else
            "Your cleaning request has been submitted. Give this passkey to your cleaner once the job is done."

        val updates = mapOf<String, Any?>(
            "bookings/$bookingId" to booking.toMap(),
            "bookingSecrets/$bookingId" to mapOf("studentId" to targetStudentId, "passkey" to passkey),
            "notifications/$targetStudentId/$notificationId" to notificationMap(
                bookingId, message, NotificationType.REQUESTED, null, passkey, date, now
            )
        )
        db.reference.updateChildren(updates).await()
    }

    /** Returns the student's uid for an External Booking ID, or null if there is no match. */
    suspend fun findStudentByExternalId(externalId: String): String? {
        val snap = ref("externalIds/${externalId.trim().uppercase()}").get().await()
        return if (snap.exists()) snap.getValue(String::class.java) else null
    }

    /**
     * Cleaner accepts a booking. The database rules only allow Pending -> In Progress, so if another cleaner
     * got there first this returns false.
     */
    suspend fun acceptBooking(booking: Booking, cleanerId: String, cleanerName: String): Boolean {
        val updated = booking.copy(status = Status.IN_PROGRESS, cleanerId = cleanerId, cleanerName = cleanerName)
        val notificationId = ref("notifications/${booking.studentId}").push().key ?: return false

        val updates = mapOf<String, Any?>(
            "bookings/${booking.id}" to updated.toMap(),
            "notifications/${booking.studentId}/$notificationId" to notificationMap(
                booking.id, "$cleanerName has accepted your cleaning request.", NotificationType.ACCEPTED,
                cleanerName, null, booking.bookingDate, System.currentTimeMillis()
            )
        )
        return try {
            db.reference.updateChildren(updates).await()
            true
        } catch (e: Exception) {
            if (isPermissionDenied(e)) false else throw e
        }
    }

    /**
     * Cleaner marks a job complete by typing the passkey the student gave them.
     * The passkey is checked by the database rules (see database.rules.json), so cleaners can never read it.
     * A wrong passkey is rejected with "permission denied" and this returns false.
     */
    suspend fun completeBooking(booking: Booking, enteredPasskey: String, cleanerName: String): Boolean {
        val updated = booking.copy(status = Status.COMPLETED, enteredPasskey = enteredPasskey.trim())
        val notificationId = ref("notifications/${booking.studentId}").push().key ?: return false

        val updates = mapOf<String, Any?>(
            "bookings/${booking.id}" to updated.toMap(),
            "notifications/${booking.studentId}/$notificationId" to notificationMap(
                booking.id, "Your cleaning has been completed by $cleanerName.", NotificationType.COMPLETED,
                cleanerName, null, booking.bookingDate, System.currentTimeMillis()
            )
        )
        return try {
            db.reference.updateChildren(updates).await()
            true
        } catch (e: Exception) {
            if (isPermissionDenied(e)) false else throw e
        }
    }

    suspend fun submitProblemReport(issue: String, room: String, photoAttached: Boolean) {
        val uid = currentUid ?: throw IllegalStateException("You are not signed in.")
        val reportRef = ref("problemReports").push()
        reportRef.setValue(
            mapOf(
                "studentId" to uid,
                "issue" to issue.trim(),
                "room" to room,
                "photoAttached" to photoAttached,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    // ---------------------------------------------------- live data (flows)
    // Sorting is done on the device. Queries are built inside the flow so any setup error is caught by .catch.

    fun observeStudentBookings(studentId: String): Flow<List<Booking>> =
        listen({ ref("bookings").orderByChild("studentId").equalTo(studentId) }) { snap ->
            snap.children.map { it.toBooking() }.sortedWith(compareByDescending<Booking> { it.bookingDate })
        }

    fun observeOpenRequests(): Flow<List<Booking>> =
        listen({ ref("bookings").orderByChild("status").equalTo(Status.PENDING) }) { snap ->
            snap.children.map { it.toBooking() }.sortedWith(compareBy<Booking> { it.bookingDate })
        }

    fun observeCleanerBookings(cleanerId: String): Flow<List<Booking>> =
        listen({ ref("bookings").orderByChild("cleanerId").equalTo(cleanerId) }) { snap ->
            snap.children.map { it.toBooking() }.sortedWith(compareBy<Booking> { it.bookingDate })
        }

    fun observeNotifications(studentId: String): Flow<List<AppNotification>> =
        listen({ ref("notifications/$studentId") }) { snap ->
            snap.children.map { it.toNotification() }.sortedByDescending { it.createdAt }
        }

    private fun <T> listen(buildQuery: () -> Query, map: (DataSnapshot) -> T): Flow<T> = callbackFlow {
        val query = buildQuery()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(map(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    // ------------------------------------------------------------- helpers

    private fun isPermissionDenied(e: Throwable): Boolean =
        e.message?.contains("permission denied", ignoreCase = true) == true

    private fun DataSnapshot.str(name: String): String? = child(name).getValue(String::class.java)

    private fun DataSnapshot.toBooking() = Booking(
        id = key.orEmpty(),
        studentId = str("studentId").orEmpty(),
        roomNumber = str("roomNumber").orEmpty(),
        bookingDate = str("bookingDate").orEmpty(),
        roomType = str("roomType").orEmpty(),
        timeSlot = str("timeSlot").orEmpty(),
        cleaningType = str("cleaningType").orEmpty(),
        specialInstructions = str("specialInstructions").orEmpty(),
        status = str("status").orEmpty(),
        cleanerId = str("cleanerId"),
        cleanerName = str("cleanerName"),
        requestedBy = str("requestedBy").orEmpty(),
        createdAt = child("createdAt").getValue(Long::class.java) ?: 0L,
        enteredPasskey = str("enteredPasskey")
    )

    private fun DataSnapshot.toNotification() = AppNotification(
        id = key.orEmpty(),
        studentId = "",
        message = str("message").orEmpty(),
        type = str("type").orEmpty(),
        cleanerName = str("cleanerName"),
        passkey = str("passkey"),
        bookingDate = str("bookingDate"),
        createdAt = child("createdAt").getValue(Long::class.java) ?: 0L
    )

    private fun Booking.toMap(): Map<String, Any?> = mapOf(
        "studentId" to studentId,
        "requestedBy" to requestedBy,
        "roomNumber" to roomNumber,
        "bookingDate" to bookingDate,
        "roomType" to roomType,
        "timeSlot" to timeSlot,
        "cleaningType" to cleaningType,
        "specialInstructions" to specialInstructions,
        "status" to status,
        "cleanerId" to cleanerId,
        "cleanerName" to cleanerName,
        "createdAt" to createdAt,
        "enteredPasskey" to enteredPasskey
    )

    private fun notificationMap(
        bookingId: String,
        message: String,
        type: String,
        cleanerName: String?,
        passkey: String?,
        bookingDate: String?,
        createdAt: Long
    ): Map<String, Any?> = mapOf(
        "bookingId" to bookingId,
        "message" to message,
        "type" to type,
        "cleanerName" to cleanerName,
        "passkey" to passkey,
        "bookingDate" to bookingDate,
        "createdAt" to createdAt
    )
}

/** Turns Firebase exceptions into messages a student can understand. */
fun friendlyError(e: Throwable): String = when (e) {
    is RoomTakenException -> e.message ?: "That room is already taken."
    is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
    is FirebaseAuthUserCollisionException ->
        "That email is already registered. Sign in the same way you registered (email/password or Google)."
    is FirebaseAuthRecentLoginRequiredException -> "Please log in again and retry."
    is FirebaseAuthWebException -> "Google sign-in was cancelled or didn't finish. Please try again."
    is FirebaseNetworkException -> "No internet connection. Please try again."
    is DatabaseException -> {
        val text = e.message.orEmpty()
        when {
            text.contains("permission denied", ignoreCase = true) ->
                "Permission denied. Make sure the Realtime Database rules have been published."
            text.contains("database url", ignoreCase = true) ->
                "Realtime Database isn't set up in this app yet. Create it in the Firebase console, then re-download " +
                        "google-services.json (or paste the URL into FirebaseConfig.kt)."
            else -> text.ifEmpty { "Database error." }
        }
    }
    else -> e.message ?: "Something went wrong."
}