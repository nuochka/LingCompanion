package com.app.lingcompanion.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.lingcompanion.R

// Adapter for RecyclerView to display messages
class MessageRVAdapter(private val messageList: ArrayList<MessageRVModal>):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // View holder for user messages
    class UserMessageViewHolder(ItemView: View): RecyclerView.ViewHolder(ItemView){
        val userMsgTV: TextView = itemView.findViewById(R.id.idTVUser)
    }

    // View holder for bot messages
    class BotMessageViewHolder(ItemView: View): RecyclerView.ViewHolder(ItemView){
        val botMsgTV: TextView = itemView.findViewById(R.id.idTVBot)
    }

    // Create view holder based on view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return if (viewType == 0) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.user_message_rv_item, parent, false)
            UserMessageViewHolder(view)
        } else {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.bot_message_rv_item, parent, false)
            BotMessageViewHolder(view)
        }
    }

    // Get total number of items in the list
    override fun getItemCount(): Int {
        return messageList.size
    }

    // Bind data to view holder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val sender = messageList[position].sender
        when(sender){
            "user" -> (holder as UserMessageViewHolder).userMsgTV.text = messageList[position].message
            "bot" -> (holder as BotMessageViewHolder).botMsgTV.text = messageList[position].message
        }
    }

    // Get view type based on sender (user or bot)
    override fun getItemViewType(position: Int): Int {
        return when(messageList[position].sender){
            "user" -> 0
            "bot" -> 1
            else -> 1
        }
    }
}