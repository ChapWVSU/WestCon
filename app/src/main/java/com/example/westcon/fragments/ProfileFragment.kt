package com.example.westcon.fragments

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.example.westcon.ui.screens.ProfileScreen
import kotlinx.coroutines.launch

class ProfileFragment : BaseFragment() {
    companion object {
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: String): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

    @Composable
    override fun ScreenContent() {
        val userId = arguments?.getString(ARG_USER_ID)
        ProfileScreen(
            userId = userId,
            onLogoutClick = {
                com.example.westcon.data.FirebaseManager.logout()
                clearBackStackAndNavigate(LandingFragment())
            },
            onBackClick = {
                parentFragmentManager.popBackStack()
            },
            onMessageClick = { authorUid, authorName ->
                val currentUid = com.example.westcon.data.FirebaseManager.getCurrentUser()?.uid ?: ""
                if (currentUid != authorUid) {
                    val chatId = if (currentUid < authorUid) "${currentUid}_${authorUid}" else "${authorUid}_$currentUid"
                    navigateTo(ChatDetailFragment.newInstance(chatId, authorUid, authorName))
                }
            },
            onExchangeClick = { authorUid, authorName ->
                val currentUid = com.example.westcon.data.FirebaseManager.getCurrentUser()?.uid ?: ""
                if (currentUid != authorUid) {
                    val chatId = if (currentUid < authorUid) "${currentUid}_${authorUid}" else "${authorUid}_$currentUid"
                    
                    lifecycleScope.launch {
                        // Send automated message
                        val introMessage = com.example.westcon.data.Message(
                            senderUid = currentUid,
                            receiverUid = authorUid,
                            text = "Hi $authorName, I'm interested in your skills. Can we discuss a potential skill exchange?",
                            timestamp = com.google.firebase.Timestamp.now()
                        )
                        com.example.westcon.data.FirebaseManager.sendMessage(introMessage, chatId)
                        
                        // Navigate to chat
                        navigateTo(ChatDetailFragment.newInstance(chatId, authorUid, authorName))
                    }
                }
            }
        )
    }
}
