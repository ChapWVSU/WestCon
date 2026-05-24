package com.example.westcon.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.DocumentId

data class SkillMastery(
    val skillName: String = "",
    val averageRating: Double = 0.0,
    val totalRatings: Int = 0,
    val level: Int = 1 // 1: Novice, 2: Intermediate, 3: Advanced, 4: Expert, 5: Guru
)

data class LearningSkill(
    val skillName: String = "",
    val rating: Double = 0.0, // 0.0 means not yet rated (N/A)
    val isDone: Boolean = false,
    val exchangeId: String? = null
)

// Firestore collection: users
// Document ID: user's uid
data class UserProfile(
    // REMOVED @DocumentId to avoid crash if 'uid' field exists in the document
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileIconName: String = "Person", // Default icon
    val department: String = "",
    val course: String = "",
    val year: String = "",
    val rating: Double = 0.0,
    val swaps: Int = 0,
    val about: String = "",
    val skillsToTeach: List<SkillMastery> = emptyList(),
    val skillsLearning: List<LearningSkill> = emptyList()
)

data class SkillExchange(
    // REMOVED @DocumentId to avoid crash if 'id' field exists in the document
    val id: String = "",
    val requesterUid: String = "",
    val responderUid: String = "",
    val skillOffered: String = "", // Skill requester is teaching
    val skillWanted: String = "", // Skill responder is teaching
    val status: String = "ACTIVE", // "ACTIVE", "DONE"
    val requesterMarkedDone: Boolean = false,
    val responderMarkedDone: Boolean = false,
    val requesterRating: Double = 0.0, // Rating requester gives to responder (for teaching skillWanted)
    val responderRating: Double = 0.0, // Rating responder gives to requester (for teaching skillOffered)
    val timestamp: Timestamp = Timestamp.now()
)

data class SkillPost(
    // REMOVED @DocumentId to avoid crash if 'id' field exists in the document
    val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorIconName: String = "Person",
    val authorMastery: Int = 1, // Proficiency level of the author in this skill
    val department: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    // Firestore documents may use `anonymous` (no `is` prefix) or `isAnonymous`.
    var anonymous: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
)

data class FreedomPost(
    // REMOVED @DocumentId to avoid crash if 'id' field exists in the document
    val id: String = "",
    val authorUid: String = "",
    val authorName: String = "User",
    val authorIconName: String = "Person",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(), // List of UIDs
    val colorHex: String = "#E3F2FD",
    val topComment: String? = null,
    val commentCount: Int = 0,
    // keep Firestore-friendly field
    var anonymous: Boolean = true
)

data class FreedomComment(
    // REMOVED @DocumentId to avoid crash if 'id' field exists in the document
    val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorName: String = "User",
    val authorIconName: String = "Person",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    var anonymous: Boolean = true
)

data class Message(
    // REMOVED @DocumentId to avoid crash if 'id' field exists in the document
    val id: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    // Firestore may store boolean fields as either "read" or "isRead".
    // We'll use isRead as the primary field.
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    // Keep read for mapping legacy documents, but rename backing field to avoid JVM clash
    @get:PropertyName("read") @set:PropertyName("read")
    var readCompat: Boolean = false
)

data class ChatSummary(
    // Document ID is otherUserUid, but it's often stored in the document too.
    val otherUserUid: String = "",
    val otherUserName: String = "",
    val otherUserIconName: String = "Person",
    val otherUserDept: String = "",
    val lastMessage: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val unreadCount: Int = 0,
    val lastMessageSenderUid: String = "",
    // Keep lastMessageRead for compatibility
    @get:PropertyName("lastMessageRead") @set:PropertyName("lastMessageRead")
    var lastMessageRead: Boolean = true,
    // Add isRead for consistency with other models
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = true,
    // Firestore documents might have a `typing` field (no `is` prefix). Keep a mutable `typing` field
    // for mapping.
    var typing: Boolean = false
)

data class Notification(
    // REMOVED @DocumentId to avoid crash if 'id' field exists in the document
    val id: String = "",
    val receiverUid: String = "",
    val type: String = "SKILL_EXCHANGE", // SKILL_EXCHANGE, ACHIEVEMENT, MESSAGE, DIGEST
    val title: String = "",
    val content: String = "",
    val senderUid: String? = null,
    val senderName: String? = null,
    val senderIconName: String = "Person",
    val senderDept: String? = null,
    val skillOffered: String? = null,
    val skillWanted: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    // Using isRead as primary field
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    // Keep read for mapping legacy documents, but rename backing field to avoid JVM clash
    @get:PropertyName("read") @set:PropertyName("read")
    var readCompat: Boolean = false
)

// Extension properties for easier access
val SkillPost.isAnonymous: Boolean get() = anonymous
val FreedomPost.isAnonymous: Boolean get() = anonymous
val Message.isActuallyRead: Boolean get() = isRead || readCompat
val Notification.isActuallyRead: Boolean get() = isRead || readCompat
val ChatSummary.isActuallyRead: Boolean get() = isRead && lastMessageRead && unreadCount == 0
val ChatSummary.isTyping: Boolean get() = typing
