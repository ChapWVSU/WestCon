package com.example.westcon.fragments

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.example.westcon.ui.screens.ChatDetailScreen

class ChatDetailFragment : BaseFragment() {
    companion object {
        private const val ARG_CHAT_ID = "chat_id"
        private const val ARG_OTHER_USER_UID = "other_user_uid"
        private const val ARG_USER_NAME = "user_name"

        fun newInstance(chatId: String, otherUserUid: String, userName: String): ChatDetailFragment {
            val fragment = ChatDetailFragment()
            val args = Bundle()
            args.putString(ARG_CHAT_ID, chatId)
            args.putString(ARG_OTHER_USER_UID, otherUserUid)
            args.putString(ARG_USER_NAME, userName)
            fragment.arguments = args
            return fragment
        }
    }

    @Composable
    override fun ScreenContent() {
        val chatId = arguments?.getString(ARG_CHAT_ID) ?: ""
        val otherUserUid = arguments?.getString(ARG_OTHER_USER_UID) ?: ""
        val userName = arguments?.getString(ARG_USER_NAME) ?: "Chat"
        
        ChatDetailScreen(
            chatId = chatId,
            otherUserUid = otherUserUid,
            otherUserName = userName,
            onBackClick = { parentFragmentManager.popBackStack() },
            onProfileClick = { uid ->
                navigateTo(ProfileFragment.newInstance(uid))
            }
        )
    }
}
