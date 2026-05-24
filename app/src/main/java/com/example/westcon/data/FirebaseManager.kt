package com.example.westcon.data

import com.example.westcon.data.FirestoreCollections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val usersCollection by lazy { db.collection(FirestoreCollections.USERS) }
    private val skillsCollection by lazy { db.collection(FirestoreCollections.SKILLS) }
    private val freedomWallCollection by lazy { db.collection(FirestoreCollections.FREEDOM_WALL) }
    private val messagesCollection by lazy { db.collection(FirestoreCollections.MESSAGES) }
    private val chatSummariesCollection by lazy { db.collection(FirestoreCollections.CHAT_SUMMARIES) }
    private val notificationsCollection by lazy { db.collection(FirestoreCollections.NOTIFICATIONS) }

    // --- Authentication ---
    fun getCurrentUser() = auth.currentUser
    
    fun isUserLoggedIn() = auth.currentUser != null

    suspend fun signUp(email: String, pass: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- User Profile ---
    suspend fun checkUsernameExists(username: String): Boolean {
        return try {
            val query = usersCollection.whereEqualTo("name", username).get().await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            android.util.Log.d("FirebaseManager", "Saving profile for ${profile.uid}, skillsLearning size: ${profile.skillsLearning.size}")
            usersCollection.document(profile.uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error saving profile for ${profile.uid}: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            android.util.Log.d("FirebaseManager", "Fetching profile for uid: $uid")
            val snapshot = usersCollection.document(uid).get().await()
            if (!snapshot.exists()) {
                android.util.Log.w("FirebaseManager", "Snapshot does not exist for uid: $uid")
                return null
            }

            val data = snapshot.data ?: return null
            android.util.Log.d("FirebaseManager", "Raw profile data keys: ${data.keys}")
            
            // Manual deserialization to handle schema migration
            val skillsLearningData = data["skillsLearning"]
            val migratedSkillsLearning = mutableListOf<LearningSkill>()
            
            if (skillsLearningData is Map<*, *>) {
                android.util.Log.d("FirebaseManager", "Found old Map format for skillsLearning: $skillsLearningData")
                // Migrate from old Map<String, Int> format
                skillsLearningData.forEach { (key, value) ->
                    if (key is String) {
                        migratedSkillsLearning.add(LearningSkill(skillName = key))
                    }
                }
            } else if (skillsLearningData is List<*>) {
                android.util.Log.d("FirebaseManager", "Found new List format for skillsLearning with ${skillsLearningData.size} items")
                // Handle new List<LearningSkill> format
                skillsLearningData.forEach { item ->
                    if (item is Map<*, *>) {
                        migratedSkillsLearning.add(LearningSkill(
                            skillName = item["skillName"] as? String ?: "",
                            rating = (item["rating"] as? Number)?.toDouble() ?: 0.0,
                            isDone = item["isDone"] as? Boolean ?: false,
                            exchangeId = item["exchangeId"] as? String
                        ))
                    }
                }
            }

            val profile = UserProfile(
                uid = data["uid"] as? String ?: uid,
                name = data["name"] as? String ?: "",
                email = data["email"] as? String ?: "",
                profileIconName = data["profileIconName"] as? String ?: "Person",
                department = data["department"] as? String ?: "",
                course = data["course"] as? String ?: "",
                year = data["year"] as? String ?: "",
                rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
                swaps = (data["swaps"] as? Number)?.toInt() ?: 0,
                about = data["about"] as? String ?: "",
                skillsToTeach = (data["skillsToTeach"] as? List<*>)?.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        SkillMastery(
                            skillName = item["skillName"] as? String ?: "",
                            averageRating = (item["averageRating"] as? Number)?.toDouble() ?: 0.0,
                            totalRatings = (item["totalRatings"] as? Number)?.toInt() ?: 0,
                            level = (item["level"] as? Number)?.toInt() ?: 1
                        )
                    } else null
                } ?: emptyList(),
                skillsLearning = migratedSkillsLearning
            )

            android.util.Log.d("FirebaseManager", "Profile successfully parsed for ${profile.name} (${profile.uid}). learningCount=${profile.skillsLearning.size}")
            profile
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error fetching/parsing profile for uid $uid: ${e.message}", e)
            null
        }
    }

    fun getAllUserProfiles(): Flow<List<UserProfile>> = callbackFlow {
        val subscription = usersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        // Reuse the parsing logic from getUserProfile for consistency
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val skillsLearningData = data["skillsLearning"]
                            val migratedSkillsLearning = mutableListOf<LearningSkill>()
                            
                            if (skillsLearningData is Map<*, *>) {
                                skillsLearningData.forEach { (key, _) ->
                                    if (key is String) migratedSkillsLearning.add(LearningSkill(skillName = key))
                                }
                            } else if (skillsLearningData is List<*>) {
                                skillsLearningData.forEach { item ->
                                    if (item is Map<*, *>) {
                                        migratedSkillsLearning.add(LearningSkill(
                                            skillName = item["skillName"] as? String ?: "",
                                            rating = (item["rating"] as? Number)?.toDouble() ?: 0.0,
                                            isDone = item["isDone"] as? Boolean ?: false,
                                            exchangeId = item["exchangeId"] as? String
                                        ))
                                    }
                                }
                            }

                            UserProfile(
                                uid = doc.id,
                                name = data["name"] as? String ?: "",
                                email = data["email"] as? String ?: "",
                                profileIconName = data["profileIconName"] as? String ?: "Person",
                                department = data["department"] as? String ?: "",
                                course = data["course"] as? String ?: "",
                                year = data["year"] as? String ?: "",
                                rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
                                swaps = (data["swaps"] as? Number)?.toInt() ?: 0,
                                about = data["about"] as? String ?: "",
                                skillsToTeach = (data["skillsToTeach"] as? List<*>)?.mapNotNull { item ->
                                    if (item is Map<*, *>) {
                                        SkillMastery(
                                            skillName = item["skillName"] as? String ?: "",
                                            averageRating = (item["averageRating"] as? Number)?.toDouble() ?: 0.0,
                                            totalRatings = (item["totalRatings"] as? Number)?.toInt() ?: 0,
                                            level = (item["level"] as? Number)?.toInt() ?: 1
                                        )
                                    } else null
                                } ?: emptyList(),
                                skillsLearning = migratedSkillsLearning
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }

    // --- Skill Exchanges ---
    private val exchangesCollection by lazy { db.collection(FirestoreCollections.EXCHANGES) }

    suspend fun createExchange(exchange: SkillExchange): Result<Unit> {
        return try {
            val ref = exchangesCollection.document()
            exchangesCollection.document(ref.id).set(exchange.copy(id = ref.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveExchange(uid1: String, uid2: String): SkillExchange? {
        return try {
            val query1 = exchangesCollection
                .whereEqualTo("requesterUid", uid1)
                .whereEqualTo("responderUid", uid2)
                .whereEqualTo("status", "ACTIVE")
                .get().await()
            
            if (!query1.isEmpty) return query1.documents[0].toObject(SkillExchange::class.java)

            val query2 = exchangesCollection
                .whereEqualTo("requesterUid", uid2)
                .whereEqualTo("responderUid", uid1)
                .whereEqualTo("status", "ACTIVE")
                .get().await()
            
            if (!query2.isEmpty) return query2.documents[0].toObject(SkillExchange::class.java)

            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun markExchangeDone(exchangeId: String, uid: String): Result<Unit> {
        return try {
            val docRef = exchangesCollection.document(exchangeId)
            val snapshot = docRef.get().await()
            val exchange = snapshot.toObject(SkillExchange::class.java) ?: return Result.failure(Exception("Exchange not found"))
            
            val isRequester = exchange.requesterUid == uid
            val updateField = if (isRequester) "requesterMarkedDone" else "responderMarkedDone"
            
            docRef.update(updateField, true).await()
            
            // Check if both are done
            val updatedSnapshot = docRef.get().await()
            val updatedExchange = updatedSnapshot.toObject(SkillExchange::class.java)!!
            if (updatedExchange.requesterMarkedDone && updatedExchange.responderMarkedDone) {
                docRef.update("status", "DONE").await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_$uid1"
    }

    suspend fun acceptExchangeRequest(notification: Notification): Result<Unit> {
        android.util.Log.d("FirebaseManager", "acceptExchangeRequest START for notification ${notification.id}")
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val senderUid = notification.senderUid ?: return Result.failure(Exception("Sender UID missing"))
            val skillOffered = notification.skillOffered ?: ""
            val skillWanted = notification.skillWanted ?: ""

            android.util.Log.d("FirebaseManager", "Exchange details: currentUid=$currentUid, senderUid=$senderUid, skillOffered=$skillOffered, skillWanted=$skillWanted")

            // 1. Create SkillExchange record FIRST
            val exchangeRef = exchangesCollection.document()
            val exchange = SkillExchange(
                id = exchangeRef.id,
                requesterUid = senderUid,
                responderUid = currentUid,
                skillOffered = skillOffered,
                skillWanted = skillWanted,
                status = "ACTIVE"
            )
            android.util.Log.d("FirebaseManager", "Creating SkillExchange document: ${exchangeRef.id}")
            exchangeRef.set(exchange).await()

            // 2. Update Profiles (Critical path)
            val senderProfile = getUserProfile(senderUid)
            val responderProfile = getUserProfile(currentUid)

            if (senderProfile != null) {
                val updatedLearning = senderProfile.skillsLearning.toMutableList()
                if (skillWanted.isNotBlank() && updatedLearning.none { it.skillName.equals(skillWanted, ignoreCase = true) }) {
                    updatedLearning.add(LearningSkill(skillName = skillWanted, exchangeId = exchange.id))
                }
                val result = saveUserProfile(senderProfile.copy(
                    skillsLearning = updatedLearning,
                    swaps = senderProfile.swaps + 1
                ))
                if (result.isFailure) throw result.exceptionOrNull() ?: Exception("Failed to update requester profile")
            } else {
                throw Exception("Requester profile not found")
            }

            if (responderProfile != null) {
                val updatedLearning = responderProfile.skillsLearning.toMutableList()
                if (skillOffered.isNotBlank() && updatedLearning.none { it.skillName.equals(skillOffered, ignoreCase = true) }) {
                    updatedLearning.add(LearningSkill(skillName = skillOffered, exchangeId = exchange.id))
                }
                val result = saveUserProfile(responderProfile.copy(
                    skillsLearning = updatedLearning,
                    swaps = responderProfile.swaps + 1
                ))
                if (result.isFailure) throw result.exceptionOrNull() ?: Exception("Failed to update responder profile")
            } else {
                throw Exception("Responder profile not found")
            }

            // 3. Send automated acceptance message (Non-critical, but should work)
            val chatId = getChatId(currentUid, senderUid)
            val acceptanceMsg = "I've accepted your exchange request! I'll teach you $skillWanted and you'll teach me $skillOffered."
            sendMessage(
                Message(
                    senderUid = currentUid,
                    receiverUid = senderUid,
                    text = acceptanceMsg,
                    timestamp = Timestamp.now()
                ),
                chatId
            )

            // 4. Delete Notification
            deleteNotification(notification.id)
            
            android.util.Log.d("FirebaseManager", "acceptExchangeRequest SUCCESS")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error in acceptExchangeRequest: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun rateUserLearning(targetUid: String, skillName: String, rating: Double, exchangeId: String): Result<Unit> {
        return try {
            val profile = getUserProfile(targetUid) ?: return Result.failure(Exception("User not found"))
            val currentUid = auth.currentUser?.uid ?: ""
            
            // Update the skill in skillsLearning
            val learningSkills = profile.skillsLearning.toMutableList()
            val skillIndex = learningSkills.indexOfFirst { it.skillName.equals(skillName, ignoreCase = true) }
            
            if (skillIndex != -1) {
                learningSkills[skillIndex] = learningSkills[skillIndex].copy(rating = rating, isDone = true)
            } else {
                learningSkills.add(LearningSkill(skillName = skillName, rating = rating, isDone = true))
            }

            // Update exchange record
            val docRef = exchangesCollection.document(exchangeId)
            val exchangeSnapshot = docRef.get().await()
            val exchange = exchangeSnapshot.toObject(SkillExchange::class.java)
            if (exchange != null) {
                if (exchange.requesterUid == currentUid) {
                    docRef.update("requesterRating", rating).await()
                } else {
                    docRef.update("responderRating", rating).await()
                }
            }

            // Recalculate Trust Score (True Mean of all ratings)
            val ratedLearning = learningSkills.filter { it.rating > 0 }
            val sumLearning = ratedLearning.sumOf { it.rating }
            val countLearning = ratedLearning.size
            
            val ratedTeaching = profile.skillsToTeach.filter { it.totalRatings > 0 }
            val sumTeaching = ratedTeaching.sumOf { it.averageRating * it.totalRatings }
            val countTeaching = ratedTeaching.sumOf { it.totalRatings }
            
            val totalCount = countLearning + countTeaching
            val finalRating = if (totalCount > 0) {
                (sumLearning + sumTeaching) / totalCount
            } else {
                0.0
            }
            
            saveUserProfile(profile.copy(skillsLearning = learningSkills, rating = finalRating))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Skill Marketplace ---
    suspend fun postSkill(post: SkillPost): Result<Unit> {
        return try {
            val ref = skillsCollection.document()
            ref.set(post.copy(id = ref.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSkillPost(postId: String): Result<Unit> {
        return try {
            skillsCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSkillPosts(): Flow<List<SkillPost>> = callbackFlow {
        val subscription = skillsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(SkillPost::class.java)?.copy(id = it.id) })
                }
            }
        awaitClose { subscription.remove() }
    }

    // --- Freedom Wall ---
    suspend fun postToFreedomWall(post: FreedomPost): Result<Unit> {
        return try {
            val ref = freedomWallCollection.document()
            val authorUid = auth.currentUser?.uid ?: ""
            val profile = getUserProfile(authorUid)
            
            ref.set(post.copy(
                id = ref.id, 
                authorUid = authorUid,
                authorName = profile?.name ?: "User",
                authorIconName = profile?.profileIconName ?: "Person"
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFreedomPost(postId: String): Result<Unit> {
        return try {
            freedomWallCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFreedomPosts(): Flow<List<FreedomPost>> = callbackFlow {
        val subscription = freedomWallCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(FreedomPost::class.java)?.copy(id = it.id) })
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun toggleLikeFreedomPost(postId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val docRef = freedomWallCollection.document(postId)
            val snapshot = docRef.get().await()
            val post = snapshot.toObject(FreedomPost::class.java)?.copy(id = snapshot.id) ?: return Result.failure(Exception("Post not found"))
            
            val likedBy = post.likedBy.toMutableList()
            var likes = post.likes
            
            if (likedBy.contains(uid)) {
                likedBy.remove(uid)
                likes--
            } else {
                likedBy.add(uid)
                likes++
            }
            
            docRef.update("likedBy", likedBy, "likes", likes).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postComment(comment: FreedomComment): Result<Unit> {
        return try {
            val postRef = freedomWallCollection.document(comment.postId)
            val commentRef = postRef.collection("comments").document()
            
            val authorUid = auth.currentUser?.uid ?: ""
            val profile = if (!comment.anonymous) getUserProfile(authorUid) else null
            
            val finalComment = comment.copy(
                id = commentRef.id,
                authorUid = authorUid,
                authorName = if (comment.anonymous) "Anonymous Taga-West" else (profile?.name ?: "User"),
                authorIconName = if (comment.anonymous) "VisibilityOff" else (profile?.profileIconName ?: "Person")
            )
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentCommentCount = snapshot.getLong("commentCount") ?: 0
                
                transaction.set(commentRef, finalComment)
                transaction.update(postRef, "commentCount", currentCommentCount + 1)
                transaction.update(postRef, "topComment", finalComment.content)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getComments(postId: String): Flow<List<FreedomComment>> = callbackFlow {
        val subscription = freedomWallCollection
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(FreedomComment::class.java)?.copy(id = it.id) })
                }
            }
        awaitClose { subscription.remove() }
    }

    // --- Messaging ---
    suspend fun sendMessage(msg: Message, chatId: String): Result<Unit> {
        return try {
            val ref = messagesCollection.document(chatId).collection("history").document()
            ref.set(msg.copy(id = ref.id)).await()
            
            // Also update chat summaries for both users
            try {
                startChat(msg.senderUid, msg.receiverUid, msg.text)
            } catch (e: Exception) {
                // Log error but don't fail the message sending
                android.util.Log.e("FirebaseManager", "Error updating chat summaries: ${e.message}", e)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun startChat(uid: String, otherUid: String, firstMsg: String) {
        try {
            updateChatSummary(uid, otherUid, firstMsg, isRecipient = false)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error updating chat summary for $uid: ${e.message}", e)
        }
        try {
            updateChatSummary(otherUid, uid, firstMsg, isRecipient = true)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error updating chat summary for $otherUid: ${e.message}", e)
        }
    }

    suspend fun markChatAsRead(otherUid: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            chatSummariesCollection
                .document(uid)
                .collection("chats")
                .document(otherUid)
                .update(
                    "unreadCount", 0, 
                    "lastMessageRead", true,
                    "isRead", true
                )
                .await()
            android.util.Log.d("FirebaseManager", "Marked chat as read for user=$uid otherUser=$otherUid")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to mark chat as read for otherUid=$otherUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markChatMessagesAsRead(chatId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            
            // Query for messages where EITHER 'read' or 'isRead' is false
            val snapshotIsRead = messagesCollection
                .document(chatId)
                .collection("history")
                .whereEqualTo("receiverUid", uid)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val snapshotRead = messagesCollection
                .document(chatId)
                .collection("history")
                .whereEqualTo("receiverUid", uid)
                .whereEqualTo("read", false)
                .get()
                .await()

            val docsById = linkedMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
            for (doc in snapshotIsRead.documents) docsById[doc.id] = doc
            for (doc in snapshotRead.documents) docsById[doc.id] = doc

            if (docsById.isEmpty()) {
                return Result.success(Unit)
            }

            val batch = db.batch()
            for ((_, doc) in docsById) {
                batch.update(doc.reference, "isRead", true, "read", true)
            }
            batch.commit().await()

            android.util.Log.d("FirebaseManager", "Marked ${docsById.size} messages as read for chat=$chatId and user=$uid")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to mark messages as read for chatId=$chatId: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun updateChatSummary(uid: String, otherUid: String, lastMsg: String, isRecipient: Boolean) {
        try {
            android.util.Log.d("FirebaseManager", "=== updateChatSummary START ===")
            android.util.Log.d("FirebaseManager", "Creating chat summary for user: $uid, with other user: $otherUid")
            
            // Try to fetch profile, but don't fail the chat summary update if it fails
            val profile = try {
                android.util.Log.d("FirebaseManager", "Attempting to fetch profile for: $otherUid")
                getUserProfile(otherUid)
            } catch (e: Exception) {
                android.util.Log.w("FirebaseManager", "Could not fetch profile for $otherUid: ${e.message}")
                null
            }

            val docRef = chatSummariesCollection.document(uid).collection("chats").document(otherUid)
            val unreadCount = if (isRecipient) {
                val existingSnapshot = docRef.get().await()
                val existingSummary = existingSnapshot.toObject(ChatSummary::class.java)?.copy(otherUserUid = existingSnapshot.id)
                (existingSummary?.unreadCount ?: 0) + 1
            } else {
                0
            }
            
            val summary = ChatSummary(
                otherUserUid = otherUid,
                otherUserName = profile?.name ?: "User",
                otherUserIconName = profile?.profileIconName ?: "Person",
                otherUserDept = profile?.department ?: "WVSU",
                lastMessage = lastMsg,
                timestamp = com.google.firebase.Timestamp.now(),
                unreadCount = unreadCount,
                lastMessageSenderUid = if (isRecipient) otherUid else uid,
                lastMessageRead = !isRecipient,
                isRead = !isRecipient
            )
            
            val path = "chat_summaries/$uid/chats/$otherUid"
            android.util.Log.d("FirebaseManager", "Writing chat summary to: $path")
            android.util.Log.d("FirebaseManager", "Summary data: otherUserName=${summary.otherUserName}, lastMessage=${summary.lastMessage}, unreadCount=${summary.unreadCount}, isRead=${summary.isRead}")
            
            docRef.set(summary).await()
            
            android.util.Log.d("FirebaseManager", "Chat summary created successfully for $uid with $otherUid")
            android.util.Log.d("FirebaseManager", "=== updateChatSummary END (SUCCESS) ===")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "=== updateChatSummary END (FAILED) ===")
            android.util.Log.e("FirebaseManager", "Failed to create chat summary for $uid: ${e.message}", e)
        }
    }

    // --- Notifications ---
    suspend fun sendNotification(notification: Notification): Result<Unit> {
        return try {
            val ref = notificationsCollection.document()
            ref.set(notification.copy(id = ref.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(id: String): Result<Unit> {
        return try {
            notificationsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(id: String): Result<Unit> {
        return try {
            // Write both field variants to support documents that use `isRead` or `read`.
            notificationsCollection.document(id).update("isRead", true, "read", true).await()
            android.util.Log.d("FirebaseManager", "Marked notification as read: id=$id")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to mark notification as read id=$id: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            // Query both possible field names (`isRead` and `read`) to support documents created by different clients.
            val snapshotIsRead = notificationsCollection
                .whereEqualTo("receiverUid", uid)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val snapshotRead = notificationsCollection
                .whereEqualTo("receiverUid", uid)
                .whereEqualTo("read", false)
                .get()
                .await()

            // Combine unique documents
            val docsById = linkedMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
            for (doc in snapshotIsRead.documents) docsById[doc.id] = doc
            for (doc in snapshotRead.documents) docsById[doc.id] = doc

            if (docsById.isEmpty()) return Result.success(Unit)

            val batch = db.batch()
            for ((_, doc) in docsById) {
                batch.update(doc.reference, "isRead", true, "read", true)
            }
            batch.commit().await()
            android.util.Log.d("FirebaseManager", "Marked all notifications as read for uid=$uid, count=${docsById.size}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to mark all notifications as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun rateUserSkill(targetUid: String, skillName: String, rating: Double): Result<Unit> {
        return try {
            val profile = getUserProfile(targetUid) ?: return Result.failure(Exception("User not found"))
            val skills = profile.skillsToTeach.toMutableList()
            val skillIndex = skills.indexOfFirst { it.skillName.equals(skillName, ignoreCase = true) }
            
            if (skillIndex != -1) {
                val skill = skills[skillIndex]
                val newTotal = skill.totalRatings + 1
                val newAvg = ((skill.averageRating * skill.totalRatings) + rating) / newTotal
                
                // Automatic Level Progression (Simple Logic: every 3 ratings = +1 level)
                val newLevel = (newTotal / 3 + 1).coerceAtMost(5)
                
                skills[skillIndex] = skill.copy(
                    averageRating = newAvg,
                    totalRatings = newTotal,
                    level = newLevel
                )
                
                // Recalculate Trust Score (True Mean of all ratings)
                val learningSkills = profile.skillsLearning
                val ratedLearning = learningSkills.filter { it.rating > 0 }
                val sumLearning = ratedLearning.sumOf { it.rating }
                val countLearning = ratedLearning.size
                
                val ratedTeaching = skills.filter { it.totalRatings > 0 }
                val sumTeaching = ratedTeaching.sumOf { it.averageRating * it.totalRatings }
                val countTeaching = ratedTeaching.sumOf { it.totalRatings }
                
                val totalCount = countLearning + countTeaching
                val finalRating = if (totalCount > 0) {
                    (sumLearning + sumTeaching) / totalCount
                } else {
                    0.0
                }

                saveUserProfile(profile.copy(skillsToTeach = skills, rating = finalRating))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        android.util.Log.d("FirebaseManager", "getMessages listener started for chatId: $chatId")
        val subscription = messagesCollection
            .document(chatId)
            .collection("history")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseManager", "getMessages error for $chatId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)?.apply {
                            val data = doc.data
                            // Handle both possible field names for robustness
                            isRead = when {
                                data?.get("isRead") is Boolean -> data["isRead"] as Boolean
                                data?.get("read") is Boolean -> data["read"] as Boolean
                                else -> isRead
                            }
                            readCompat = isRead
                        }
                    }
                    android.util.Log.d("FirebaseManager", "getMessages snapshot update for $chatId: ${list.size} messages")
                    trySend(list)
                }
            }
        awaitClose { 
            android.util.Log.d("FirebaseManager", "getMessages listener closed for chatId: $chatId")
            subscription.remove() 
        }
    }

    fun getChatSummaries(): Flow<List<ChatSummary>> = callbackFlow {
        val uid = auth.currentUser?.uid
        android.util.Log.d("FirebaseManager", "getChatSummaries listener started for uid: $uid")
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = chatSummariesCollection
            .document(uid)
            .collection("chats")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseManager", "getChatSummaries error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatSummary::class.java)?.copy(otherUserUid = doc.id)?.apply {
                            val data = doc.data
                            // Robust unread detection
                            isRead = when {
                                data?.get("isRead") is Boolean -> data["isRead"] as Boolean
                                data?.get("lastMessageRead") is Boolean -> data["lastMessageRead"] as Boolean
                                else -> isRead
                            }
                            lastMessageRead = isRead
                            
                            // Log raw data for debugging
                            android.util.Log.d("FirebaseManager", "Summary raw data: ${doc.id} -> $data")
                        }
                    }
                    android.util.Log.d("FirebaseManager", "getChatSummaries snapshot update: ${list.size} summaries")
                    trySend(list)
                }
            }
        awaitClose { 
            android.util.Log.d("FirebaseManager", "getChatSummaries listener closed")
            subscription.remove() 
        }
    }

    // --- Notifications ---
    fun getNotifications(): Flow<List<Notification>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // Primary listener: ordered by timestamp (server-side). If Firestore requires
        // a composite index and fails, fallback to an unordered listener and sort
        // client-side to preserve UX without needing immediate index creation.
        var subscription: com.google.firebase.firestore.ListenerRegistration? = null

        subscription = notificationsCollection
            .whereEqualTo("receiverUid", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("FirebaseManager", "Notifications query failed: ${error.message}")
                    // Detect index-required failure and attach unordered fallback
                    if (error.message?.contains("requires an index") == true || error.message?.contains("index") == true) {
                        try {
                            // remove the failing registration and attach fallback
                            subscription?.remove()
                        } catch (t: Throwable) { /* ignore */ }

                        subscription = notificationsCollection
                            .whereEqualTo("receiverUid", uid)
                            .addSnapshotListener { snap2, err2 ->
                                if (err2 != null) {
                                    android.util.Log.e("FirebaseManager", "Fallback notifications listener failed: ${err2.message}", err2)
                                    return@addSnapshotListener
                                }
                                if (snap2 != null) {
                                    val list = snap2.documents.mapNotNull { doc ->
                                        doc.toObject(Notification::class.java)?.copy(id = doc.id)?.apply {
                                            val docMap = doc.data
                                            readCompat = when {
                                                docMap?.get("read") is Boolean -> docMap["read"] as Boolean
                                                docMap?.get("isRead") is Boolean -> docMap["isRead"] as Boolean
                                                else -> readCompat
                                            }
                                        }
                                    }.sortedByDescending { it.timestamp }
                                    trySend(list)
                                }
                            }
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)?.apply {
                            val docMap = doc.data
                            readCompat = when {
                                docMap?.get("read") is Boolean -> docMap["read"] as Boolean
                                docMap?.get("isRead") is Boolean -> docMap["isRead"] as Boolean
                                else -> readCompat
                            }
                        }
                    }
                    trySend(list)
                }
            }

        awaitClose { try { subscription?.remove() } catch (t: Throwable) { } }
    }
}
