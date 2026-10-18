package com.southpoint.crms.data

/** Role strings stored in Firestore (users/{uid}.role). */
object Roles {
    const val STUDENT = "student"
    const val CLEANER = "cleaner"
}

/** Booking status values (same strings the original SQL table used). */
object Status {
    const val PENDING = "Pending"
    const val IN_PROGRESS = "In Progress"
    const val COMPLETED = "Completed"
}

object NotificationType {
    const val REQUESTED = "Requested"
    const val ACCEPTED = "Accepted"
    const val COMPLETED = "Completed"
}

/** Dropdown options copied from the original Razor views. */
object Options {
    /** Floors 1-5, rooms x01..x10 -> 101..110, 201..210, ... 510 */
    val roomsByFloor: List<Pair<String, List<String>>> = (1..5).map { floor ->
        "Floor $floor" to (1..10).map { n -> (floor * 100 + n).toString() }
    }

    /** Flat list for the dropdowns: 101..110, 201..210, ... 510 */
    val allRooms: List<String> = roomsByFloor.flatMap { it.second }

    val roomTypes = listOf("Single", "Sharing", "Bachelor")

    val timeSlots = listOf(
        "8:30 AM - 10:00 AM",
        "10:00 AM - 12:00 PM",
        "12:30 PM - 2:00 PM",
        "2:00 PM - 4:00 PM"
    )

    val cleaningTypes = listOf("Regular", "Deep", "Move-in/Move-out")
}

/** A signed-in user (student or cleaner). Mirrors the Student / CleaningStaff tables. */
data class UserProfile(
    val uid: String,
    val role: String,
    val name: String,
    val email: String,
    val room: String = "",
    val externalBookingId: String = ""
)

/** Mirrors the Booking table + BookingViewModel. */
data class Booking(
    val id: String,
    val studentId: String,
    val roomNumber: String,
    val bookingDate: String,
    val roomType: String,
    val timeSlot: String,
    val cleaningType: String,
    val specialInstructions: String,
    val status: String,
    val cleanerId: String?,
    val cleanerName: String?,
    val requestedBy: String = "",
    val createdAt: Long = 0L,
    val enteredPasskey: String? = null
)

/** Mirrors the Notification table + NotificationViewModel. */
data class AppNotification(
    val id: String,
    val studentId: String,
    val message: String,
    val type: String,
    val cleanerName: String?,
    val passkey: String?,
    val bookingDate: String?,
    val createdAt: Long
)
