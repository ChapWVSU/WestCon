package com.example.westcon.data

import com.example.westcon.data.FirestoreCollections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) { Result.failure(e) }
    }

    fun logout() = auth.signOut()

    suspend fun sendEmailVerification(): Result<Unit> {
        return try { auth.currentUser?.sendEmailVerification()?.await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reloadUser(): Result<Unit> {
        return try { auth.currentUser?.reload()?.await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try { auth.sendPasswordResetEmail(email).await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    // --- User Profile ---
    suspend fun checkUsernameExists(username: String): Boolean {
        return try { val query = usersCollection.whereEqualTo("name", username).get().await(); !query.isEmpty } catch (e: Exception) { false }
    }

    fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val subscription = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    trySend(snapshot.toObject(UserProfile::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try { usersCollection.document(profile.uid).set(profile).await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun isProfileComplete(uid: String): Boolean {
        return try { usersCollection.document(uid).get().await().exists() } catch (e: Exception) { false }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (!snapshot.exists()) return null
            val data = snapshot.data ?: return null
            
            val skillsLearningData = data["skillsLearning"]
            val migratedSkillsLearning = mutableListOf<LearningSkill>()
            
            if (skillsLearningData is Map<*, *>) {
                skillsLearningData.forEach { (key, _) -> if (key is String) migratedSkillsLearning.add(LearningSkill(skillName = key)) }
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
        } catch (e: Exception) { null }
    }

    fun getAllUserProfiles(): Flow<List<UserProfile>> = callbackFlow {
        val subscription = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        val skillsLearningData = data["skillsLearning"]
                        val migratedSkillsLearning = mutableListOf<LearningSkill>()
                        if (skillsLearningData is List<*>) {
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
                    } catch (e: Exception) { null }
                }
                trySend(list)
            }
        }
        awaitClose { subscription.remove() }
    }

    // --- Skill Exchanges ---
    private val exchangesCollection by lazy { db.collection(FirestoreCollections.EXCHANGES) }

    fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_$uid1"
    }

    suspend fun getActiveExchange(uid1: String, uid2: String): SkillExchange? {
        return try {
            val query1 = exchangesCollection.whereEqualTo("requesterUid", uid1).whereEqualTo("responderUid", uid2).whereEqualTo("status", "ACTIVE").get().await()
            if (!query1.isEmpty) return query1.documents[0].toObject(SkillExchange::class.java)
            val query2 = exchangesCollection.whereEqualTo("requesterUid", uid2).whereEqualTo("responderUid", uid1).whereEqualTo("status", "ACTIVE").get().await()
            if (!query2.isEmpty) return query2.documents[0].toObject(SkillExchange::class.java)
            null
        } catch (e: Exception) { null }
    }

    suspend fun markExchangeDone(exchangeId: String, uid: String): Result<Unit> {
        return try {
            val docRef = exchangesCollection.document(exchangeId)
            val snapshot = docRef.get().await()
            val exchange = snapshot.toObject(SkillExchange::class.java) ?: return Result.failure(Exception("Exchange not found"))
            val isRequester = exchange.requesterUid == uid
            val updateField = if (isRequester) "requesterMarkedDone" else "responderMarkedDone"
            docRef.update(updateField, true).await()
            val profile = getUserProfile(uid)
            if (profile != null) {
                val skillLearned = if (isRequester) exchange.skillWanted else exchange.skillOffered
                val updatedLearning = profile.skillsLearning.map { if (it.skillName.equals(skillLearned, ignoreCase = true)) it.copy(isDone = true) else it }
                saveUserProfile(profile.copy(skillsLearning = updatedLearning))
            }
            val updatedSnapshot = docRef.get().await()
            val updatedExchange = updatedSnapshot.toObject(SkillExchange::class.java)!!
            if (updatedExchange.requesterMarkedDone && updatedExchange.responderMarkedDone) docRef.update("status", "DONE").await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun submitExchangeRating(
        exchangeId: String,
        targetUid: String,
        teachingRating: Double,
        learningRating: Double,
        taughtSkillName: String,
        learnedSkillName: String
    ): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val exDoc = exchangesCollection.document(exchangeId)
            val snapshot = exDoc.get().await()
            val exchange = snapshot.toObject(SkillExchange::class.java) ?: return Result.failure(Exception("Exchange not found"))

            if (exchange.requesterUid == currentUid && exchange.requesterRated) return Result.failure(Exception("Already rated"))
            if (exchange.responderUid == currentUid && exchange.responderRated) return Result.failure(Exception("Already rated"))

            val isRequester = exchange.requesterUid == currentUid
            val prefix = if (isRequester) "requester" else "responder"
            val updates = mapOf(
                "${prefix}Rated" to true,
                if (isRequester) "responderTeachingRating" to teachingRating else "requesterTeachingRating" to teachingRating,
                if (isRequester) "responderLearningRating" to learningRating else "requesterLearningRating" to learningRating
            )
            exDoc.update(updates).await()

            val profile = getUserProfile(targetUid) ?: return Result.failure(Exception("User not found"))
            val teachSkills = profile.skillsToTeach.toMutableList()
            val teachIdx = teachSkills.indexOfFirst { it.skillName.equals(taughtSkillName, ignoreCase = true) }
            if (teachIdx != -1) {
                val s = teachSkills[teachIdx]; val nTotal = s.totalRatings + 1; val nAvg = ((s.averageRating * s.totalRatings) + teachingRating) / nTotal
                val nLevel = when { nAvg >= 4.5 && nTotal >= 15 -> 5; nAvg >= 4.0 && nTotal >= 10 -> 4; nAvg >= 3.5 && nTotal >= 6 -> 3; nAvg >= 3.0 && nTotal >= 3 -> 2; else -> 1 }
                teachSkills[teachIdx] = s.copy(averageRating = nAvg, totalRatings = nTotal, level = nLevel)
            }

            val learnSkills = profile.skillsLearning.toMutableList()
            val learnIdx = learnSkills.indexOfFirst { it.skillName.equals(learnedSkillName, ignoreCase = true) }
            if (learnIdx != -1) learnSkills[learnIdx] = learnSkills[learnIdx].copy(rating = learningRating, isDone = true)
            else learnSkills.add(LearningSkill(skillName = learnedSkillName, rating = learningRating, isDone = true))

            val rLearning = learnSkills.filter { it.rating > 0 }; val sumL = rLearning.sumOf { it.rating }
            val rTeaching = teachSkills.filter { it.totalRatings > 0 }; val sumT = rTeaching.sumOf { it.averageRating * it.totalRatings }
            val totalC = rLearning.size + rTeaching.sumOf { it.totalRatings }
            val fRating = if (totalC > 0) (sumL + sumT) / totalC else 0.0

            saveUserProfile(profile.copy(skillsToTeach = teachSkills, skillsLearning = learnSkills, rating = fRating))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getRelevantExchangeFlow(currentUid: String, otherUid: String): Flow<SkillExchange?> = callbackFlow {
        var exchanges1: List<SkillExchange>? = null
        var exchanges2: List<SkillExchange>? = null

        fun sendRelevant() {
            if (exchanges1 == null || exchanges2 == null) return
            val combined = (exchanges1!! + exchanges2!!)
            
            // Priority Logic:
            // 1. The latest ACTIVE exchange (Ongoing session)
            // 2. The latest DONE exchange that hasn't been rated by the current user
            // 3. The absolute latest exchange (Fallback)
            
            val active = combined.filter { it.status == "ACTIVE" }.sortedByDescending { it.timestamp }.firstOrNull()
            if (active != null) {
                trySend(active)
                return
            }
            
            val unrated = combined.filter { 
                it.status == "DONE" && (if (it.requesterUid == currentUid) !it.requesterRated else !it.responderRated) 
            }.sortedByDescending { it.timestamp }.firstOrNull()
            
            if (unrated != null) {
                trySend(unrated)
                return
            }
            
            trySend(combined.sortedByDescending { it.timestamp }.firstOrNull())
        }

        val listener1 = exchangesCollection
            .whereEqualTo("requesterUid", currentUid)
            .whereEqualTo("responderUid", otherUid)
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    exchanges1 = snapshot?.documents?.mapNotNull { it.toObject(SkillExchange::class.java)?.copy(id = it.id) } ?: emptyList()
                    sendRelevant()
                }
            }

        val listener2 = exchangesCollection
            .whereEqualTo("requesterUid", otherUid)
            .whereEqualTo("responderUid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    exchanges2 = snapshot?.documents?.mapNotNull { it.toObject(SkillExchange::class.java)?.copy(id = it.id) } ?: emptyList()
                    sendRelevant()
                }
            }
        
        awaitClose { 
            listener1.remove()
            listener2.remove()
        }
    }

    suspend fun acceptExchangeRequest(notification: Notification): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val senderUid = notification.senderUid ?: return Result.failure(Exception("Sender UID missing"))
            val skillOffered = notification.skillOffered ?: ""
            val skillWanted = notification.skillWanted ?: ""

            // STRICT ENFORCEMENT: Check for existing active exchange
            val existing = getActiveExchange(currentUid, senderUid)
            if (existing != null) {
                return Result.failure(Exception("You already have an active exchange with this user. Please complete it first."))
            }

            val exchangeRef = exchangesCollection.document()
            val exchange = SkillExchange(
                id = exchangeRef.id,
                requesterUid = senderUid,
                responderUid = currentUid,
                skillOffered = skillOffered,
                skillWanted = skillWanted,
                status = "ACTIVE"
            )
            exchangeRef.set(exchange).await()

            val senderProfile = getUserProfile(senderUid)
            val responderProfile = getUserProfile(currentUid)

            if (senderProfile != null) {
                val updatedLearning = senderProfile.skillsLearning.toMutableList()
                if (skillWanted.isNotBlank() && updatedLearning.none { it.skillName.equals(skillWanted, ignoreCase = true) }) {
                    updatedLearning.add(LearningSkill(skillName = skillWanted, exchangeId = exchange.id))
                }
                saveUserProfile(senderProfile.copy(skillsLearning = updatedLearning, swaps = senderProfile.swaps + 1))
            }

            if (responderProfile != null) {
                val updatedLearning = responderProfile.skillsLearning.toMutableList()
                if (skillOffered.isNotBlank() && updatedLearning.none { it.skillName.equals(skillOffered, ignoreCase = true) }) {
                    updatedLearning.add(LearningSkill(skillName = skillOffered, exchangeId = exchange.id))
                }
                saveUserProfile(responderProfile.copy(skillsLearning = updatedLearning, swaps = responderProfile.swaps + 1))
            }

            val chatId = getChatId(currentUid, senderUid)
            sendMessage(Message(senderUid = currentUid, receiverUid = senderUid, text = "I've accepted your exchange request! I'll teach you $skillWanted and you'll teach me $skillOffered."), chatId)
            deleteNotification(notification.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Presence ---
    fun updateOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("online", isOnline, "lastActive", com.google.firebase.Timestamp.now())
    }

    fun getUserOnlineStatus(uid: String): Flow<Boolean> = callbackFlow {
        val subscription = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                trySend(snapshot.getBoolean("online") ?: false)
            }
        }
        awaitClose { subscription.remove() }
    }

    // --- Skill Marketplace ---
    suspend fun postSkill(post: SkillPost): Result<Unit> {
        return try { val ref = skillsCollection.document(); ref.set(post.copy(id = ref.id)).await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteSkillPost(postId: String): Result<Unit> {
        return try { skillsCollection.document(postId).delete().await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    fun getSkillPosts(): Flow<List<SkillPost>> = callbackFlow {
        val subscription = skillsCollection.orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error -> if (error == null && snapshot != null) trySend(snapshot.documents.mapNotNull { it.toObject(SkillPost::class.java)?.copy(id = it.id) }) }
        awaitClose { subscription.remove() }
    }

    // --- Freedom Wall ---
    suspend fun postToFreedomWall(post: FreedomPost): Result<Unit> {
        return try {
            val authorUid = auth.currentUser?.uid ?: ""; val profile = getUserProfile(authorUid)
            val ref = freedomWallCollection.document(); ref.set(post.copy(id = ref.id, authorUid = authorUid, authorName = profile?.name ?: "User", authorIconName = profile?.profileIconName ?: "Person")).await(); Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteFreedomPost(postId: String): Result<Unit> {
        return try { freedomWallCollection.document(postId).delete().await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    fun getFreedomPosts(): Flow<List<FreedomPost>> = callbackFlow {
        val subscription = freedomWallCollection.orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error -> if (error == null && snapshot != null) trySend(snapshot.documents.mapNotNull { it.toObject(FreedomPost::class.java)?.copy(id = it.id) }) }
        awaitClose { subscription.remove() }
    }

    suspend fun toggleLikeFreedomPost(postId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val docRef = freedomWallCollection.document(postId); val snapshot = docRef.get().await(); val post = snapshot.toObject(FreedomPost::class.java)?.copy(id = snapshot.id) ?: return Result.failure(Exception("Post not found"))
            val likedBy = post.likedBy.toMutableList(); var likes = post.likes
            if (likedBy.contains(uid)) { likedBy.remove(uid); likes-- } else { likedBy.add(uid); likes++ }
            docRef.update("likedBy", likedBy, "likes", likes).await(); Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun postComment(comment: FreedomComment): Result<Unit> {
        return try {
            val postRef = freedomWallCollection.document(comment.postId); val commentRef = postRef.collection("comments").document(); val authorUid = auth.currentUser?.uid ?: ""; val profile = if (!comment.anonymous) getUserProfile(authorUid) else null
            val finalComment = comment.copy(id = commentRef.id, authorUid = authorUid, authorName = if (comment.anonymous) "Anonymous Taga-West" else (profile?.name ?: "User"), authorIconName = if (comment.anonymous) "VisibilityOff" else (profile?.profileIconName ?: "Person"))
            db.runTransaction { transaction -> val snapshot = transaction.get(postRef); val currentCommentCount = snapshot.getLong("commentCount") ?: 0; transaction.set(commentRef, finalComment); transaction.update(postRef, "commentCount", currentCommentCount + 1); transaction.update(postRef, "topComment", finalComment.content) }.await(); Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getComments(postId: String): Flow<List<FreedomComment>> = callbackFlow {
        val subscription = freedomWallCollection.document(postId).collection("comments").orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshot, error -> if (error == null && snapshot != null) trySend(snapshot.documents.mapNotNull { it.toObject(FreedomComment::class.java)?.copy(id = it.id) }) }
        awaitClose { subscription.remove() }
    }

    // --- Messaging ---
    suspend fun sendMessage(msg: Message, chatId: String): Result<Unit> {
        return try { val ref = messagesCollection.document(chatId).collection("history").document(); ref.set(msg.copy(id = ref.id)).await(); startChat(msg.senderUid, msg.receiverUid, msg.text); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun startChat(uid: String, otherUid: String, firstMsg: String) {
        updateChatSummary(uid, otherUid, firstMsg, isRecipient = false); updateChatSummary(otherUid, uid, firstMsg, isRecipient = true)
    }

    suspend fun markChatAsRead(otherUid: String): Result<Unit> {
        return try { val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in")); chatSummariesCollection.document(uid).collection("chats").document(otherUid).update("unreadCount", 0, "lastMessageRead", true, "isRead", true).await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun markChatMessagesAsRead(chatId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val snapshotIsRead = messagesCollection.document(chatId).collection("history").whereEqualTo("receiverUid", uid).whereEqualTo("isRead", false).get().await()
            val snapshotRead = messagesCollection.document(chatId).collection("history").whereEqualTo("receiverUid", uid).whereEqualTo("read", false).get().await()
            val docsById = linkedMapOf<String, com.google.firebase.firestore.DocumentSnapshot>(); for (doc in snapshotIsRead.documents) docsById[doc.id] = doc; for (doc in snapshotRead.documents) docsById[doc.id] = doc
            if (docsById.isEmpty()) return Result.success(Unit)
            val batch = db.batch(); for ((_, doc) in docsById) batch.update(doc.reference, "isRead", true, "read", true); batch.commit().await(); Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun updateChatSummary(uid: String, otherUid: String, lastMsg: String, isRecipient: Boolean) {
        try {
            val profile = try { getUserProfile(otherUid) } catch (e: Exception) { null }
            val docRef = chatSummariesCollection.document(uid).collection("chats").document(otherUid)
            val unreadCount = if (isRecipient) { val existingSnapshot = docRef.get().await(); val existingSummary = existingSnapshot.toObject(ChatSummary::class.java); (existingSummary?.unreadCount ?: 0) + 1 } else 0
            val summary = ChatSummary(otherUserUid = otherUid, otherUserName = profile?.name ?: "User", otherUserIconName = profile?.profileIconName ?: "Person", otherUserDept = profile?.department ?: "WVSU", lastMessage = lastMsg, timestamp = com.google.firebase.Timestamp.now(), unreadCount = unreadCount, lastMessageSenderUid = if (isRecipient) otherUid else uid, lastMessageRead = !isRecipient, isRead = !isRecipient)
            docRef.set(summary).await()
        } catch (e: Exception) { }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val subscription = messagesCollection.document(chatId).collection("history").orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshot, error -> if (error == null && snapshot != null) trySend(snapshot.documents.mapNotNull { doc -> doc.toObject(Message::class.java)?.copy(id = doc.id)?.apply { val data = doc.data; isRead = when { data?.get("isRead") is Boolean -> data["isRead"] as Boolean; data?.get("read") is Boolean -> data["read"] as Boolean; else -> isRead }; readCompat = isRead } }) }
        awaitClose { subscription.remove() }
    }

    fun getChatSummaries(): Flow<List<ChatSummary>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = chatSummariesCollection.document(uid).collection("chats").orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error -> if (error == null && snapshot != null) trySend(snapshot.documents.mapNotNull { doc -> doc.toObject(ChatSummary::class.java)?.copy(otherUserUid = doc.id)?.apply { val data = doc.data; isRead = when { data?.get("isRead") is Boolean -> data["isRead"] as Boolean; data?.get("lastMessageRead") is Boolean -> data["lastMessageRead"] as Boolean; else -> isRead }; lastMessageRead = isRead } }) }
        awaitClose { subscription.remove() }
    }

    suspend fun setUserTypingStatus(otherUid: String, isTyping: Boolean): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            // We update the typing status in the OTHER user's summary of this chat
            chatSummariesCollection.document(otherUid).collection("chats").document(uid)
                .update("typing", isTyping).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getTypingStatus(otherUid: String): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        // We listen to our own summary of this chat to see if the other user is typing
        val subscription = chatSummariesCollection.document(uid).collection("chats").document(otherUid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val typing = snapshot.getBoolean("typing") ?: false
                    trySend(typing)
                }
            }
        awaitClose { subscription.remove() }
    }

    // --- Notifications ---
    suspend fun sendNotification(notification: Notification): Result<Unit> { return try { val ref = notificationsCollection.document(); ref.set(notification.copy(id = ref.id)).await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) } }
    suspend fun deleteNotification(id: String): Result<Unit> { return try { notificationsCollection.document(id).delete().await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) } }
    suspend fun markNotificationAsRead(id: String): Result<Unit> { return try { notificationsCollection.document(id).update("isRead", true, "read", true).await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) } }
    suspend fun markAllNotificationsAsRead(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in")); val snapshotIsRead = notificationsCollection.whereEqualTo("receiverUid", uid).whereEqualTo("isRead", false).get().await(); val snapshotRead = notificationsCollection.whereEqualTo("receiverUid", uid).whereEqualTo("read", false).get().await()
            val docsById = linkedMapOf<String, com.google.firebase.firestore.DocumentSnapshot>(); for (doc in snapshotIsRead.documents) docsById[doc.id] = doc; for (doc in snapshotRead.documents) docsById[doc.id] = doc
            if (docsById.isEmpty()) return Result.success(Unit)
            val batch = db.batch(); for ((_, doc) in docsById) batch.update(doc.reference, "isRead", true, "read", true); batch.commit().await(); Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getNotifications(): Flow<List<Notification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        var sub: com.google.firebase.firestore.ListenerRegistration? = null
        sub = notificationsCollection.whereEqualTo("receiverUid", uid).orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (error != null && (error.message?.contains("index") == true)) {
                sub?.remove(); sub = notificationsCollection.whereEqualTo("receiverUid", uid).addSnapshotListener { snap2, err2 -> if (err2 == null && snap2 != null) trySend(snap2.documents.mapNotNull { doc -> doc.toObject(Notification::class.java)?.copy(id = doc.id)?.apply { val docMap = doc.data; readCompat = if (docMap?.get("read") is Boolean) docMap["read"] as Boolean else if (docMap?.get("isRead") is Boolean) docMap["isRead"] as Boolean else readCompat } }.sortedByDescending { it.timestamp }) }
            } else if (snapshot != null) trySend(snapshot.documents.mapNotNull { doc -> doc.toObject(Notification::class.java)?.copy(id = doc.id)?.apply { val docMap = doc.data; readCompat = if (docMap?.get("read") is Boolean) docMap["read"] as Boolean else if (docMap?.get("isRead") is Boolean) docMap["isRead"] as Boolean else readCompat } })
        }
        awaitClose { sub?.remove() }
    }
}
