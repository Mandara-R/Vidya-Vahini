package com.vidyavahini.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.vidyavahini.app.data.model.Breakdown
import com.vidyavahini.app.data.model.BusPing
import com.vidyavahini.app.data.model.Route
import com.vidyavahini.app.data.model.Student

/**
 * Single source of truth for all Firebase Realtime Database operations.
 * All reads/writes pass through this class — ViewModels never touch Firebase directly.
 */
class FirebaseRepository {

    private val db = FirebaseDatabase.getInstance(
        "https://vidyavahini-6e45b-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).reference
    private val auth = FirebaseAuth.getInstance()

    // ── Student ─────────────────────────────────────────────────────────────

    fun saveStudent(student: Student) {
        val uid = auth.currentUser?.uid ?: return

        db.child("students")
            .child(uid)
            .setValue(student)

            .addOnSuccessListener {
                android.util.Log.d(
                    "Firebase",
                    "Student saved successfully"
                )
            }

            .addOnFailureListener { e ->
                android.util.Log.e(
                    "FirebaseRepo",
                    "Error saving student",
                    e
                )
            }
    }

    /** Fetches the student profile once (not a real-time listener). */
    fun getStudent(onResult: (Student?) -> Unit) {

        val uid = auth.currentUser?.uid ?: return onResult(null)

        db.child("students")
            .child(uid)

            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snap: DataSnapshot) {
                    onResult(snap.getValue(Student::class.java))
                }

                override fun onCancelled(e: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // ── Route ──────────────────────────────────────────────────────────────  ──

    /** Fetches a single route object by its ID. */
    fun getRoute(routeId: String, onResult: (Route?) -> Unit) {

        db.child("routes")
            .child(routeId)

            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snap: DataSnapshot) {
                    onResult(snap.getValue(Route::class.java))
                }

                override fun onCancelled(e: DatabaseError) {
                    onResult(null)
                }
            })
    }

    /** Fetches all available routes — used in RegisterFragment dropdown. */
    fun getAllRoutes(onResult: (Map<String, Route>) -> Unit) {

        db.child("routes")

            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snap: DataSnapshot) {

                    val map = mutableMapOf<String, Route>()

                    snap.children.forEach { child ->

                        child.getValue(Route::class.java)?.let {
                            map[child.key!!] = it
                        }
                    }

                    onResult(map)
                }

                override fun onCancelled(e: DatabaseError) {
                    onResult(emptyMap())
                }
            })
    }

    /** Seeds dummy BMTC routes into Firebase for testing if database is empty. */
    fun seedDummyRoutes(onComplete: () -> Unit) {

        val dummyRoutes = mapOf(

            "route_500d" to Route(
                name = "500-D: Central Silk Board → Hebbal",

                stops = mapOf(

                    "stop_01" to com.vidyavahini.app.data.model.Stop(
                        "Central Silk Board",
                        12.9176,
                        77.6238,
                        1
                    ),

                    "stop_02" to com.vidyavahini.app.data.model.Stop(
                        "HSR Layout",
                        12.9121,
                        77.6446,
                        2
                    ),

                    "stop_03" to com.vidyavahini.app.data.model.Stop(
                        "Agara Junction",
                        12.9234,
                        77.6501,
                        3
                    ),

                    "stop_04" to com.vidyavahini.app.data.model.Stop(
                        "Bellandur Gate",
                        12.9274,
                        77.6698,
                        4
                    ),

                    "stop_05" to com.vidyavahini.app.data.model.Stop(
                        "Marathahalli Bridge",
                        12.9553,
                        77.6984,
                        5
                    ),

                    "stop_06" to com.vidyavahini.app.data.model.Stop(
                        "Kalyan Nagar",
                        13.0280,
                        77.6399,
                        6
                    ),

                    "stop_07" to com.vidyavahini.app.data.model.Stop(
                        "Hebbal",
                        13.0354,
                        77.5988,
                        7
                    )
                )
            )
        )

        db.child("routes")
            .setValue(dummyRoutes)

            .addOnSuccessListener {

                android.util.Log.d(
                    "Firebase",
                    "Routes added SUCCESSFULLY"
                )

                onComplete()
            }

            .addOnFailureListener { e ->

                android.util.Log.e(
                    "Firebase",
                    "Routes FAILED",
                    e
                )
            }
    }

    // ── Ping ─────────────────────────────────────────────────────────────────

    fun pingBus(routeId: String, stopId: String) {

        val uid = auth.currentUser?.uid ?: return

        val ping = BusPing(
            stopId = stopId,
            timestamp = System.currentTimeMillis(),
            pinggedBy = uid,
            status = "on_time"
        )

        db.child("pings")
            .child(routeId)
            .child("latest")
            .setValue(ping)

            .addOnSuccessListener {
                android.util.Log.d(
                    "Firebase",
                    "Ping sent successfully"
                )
            }

            .addOnFailureListener { e ->
                android.util.Log.e(
                    "FirebaseRepo",
                    "Error pinging bus",
                    e
                )
            }
    }

    fun listenForPings(
        routeId: String,
        onUpdate: (BusPing) -> Unit
    ): ValueEventListener {

        val listener = object : ValueEventListener {

            override fun onDataChange(snap: DataSnapshot) {

                snap.getValue(BusPing::class.java)?.let {
                    onUpdate(it)
                }
            }

            override fun onCancelled(e: DatabaseError) {}
        }

        db.child("pings")
            .child(routeId)
            .child("latest")
            .addValueEventListener(listener)

        return listener
    }

    fun removePingListener(
        routeId: String,
        listener: ValueEventListener
    ) {

        db.child("pings")
            .child(routeId)
            .child("latest")
            .removeEventListener(listener)
    }

    // ── Breakdown ────────────────────────────────────────────────────────────

    fun reportBreakdown(routeId: String, message: String) {

        val uid = auth.currentUser?.uid ?: return

        val breakdown = Breakdown(
            active = true,
            reportedBy = uid,
            timestamp = System.currentTimeMillis(),
            message = message
        )

        db.child("breakdowns")
            .child(routeId)
            .setValue(breakdown)
    }

    fun clearBreakdown(routeId: String) {

        val uid = auth.currentUser?.uid ?: return

        val cleared = Breakdown(
            active = false,
            reportedBy = uid,
            timestamp = System.currentTimeMillis(),
            message = ""
        )

        db.child("breakdowns")
            .child(routeId)
            .setValue(cleared)
    }

    fun listenForBreakdown(
        routeId: String,
        onUpdate: (Breakdown?) -> Unit
    ) {

        db.child("breakdowns")
            .child(routeId)

            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snap: DataSnapshot) {
                    onUpdate(snap.getValue(Breakdown::class.java))
                }

                override fun onCancelled(e: DatabaseError) {
                    onUpdate(null)
                }
            })
    }

    // ── Safe Reach ───────────────────────────────────────────────────────────

    fun markSafeReach(studentName: String) {

        val uid = auth.currentUser?.uid ?: return

        db.child("safereach")
            .child(uid)

            .setValue(
                mapOf(
                    "studentName" to studentName,
                    "timestamp" to System.currentTimeMillis(),
                    "reached" to true
                )
            )
    }
}