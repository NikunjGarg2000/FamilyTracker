package com.zyrosite.nikunjgarg.familyTrackerApp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zyrosite.nikunjgarg.familyTrackerApp.data.model.UserContact
import com.zyrosite.nikunjgarg.familyTrackerApp.databinding.ContactTicketBinding
import com.zyrosite.nikunjgarg.familyTrackerApp.ui.viewholder.ContactViewHolder

class ContactAdapter(
    private val contactList: ArrayList<UserContact>,
    private val onItemClicked: (UserContact) -> Unit
) : RecyclerView.Adapter<ContactViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding =
            ContactTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding) {
            onItemClicked(contactList[it])
        }
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) =
        holder.bind(contactList[position])

    override fun getItemCount(): Int = contactList.size
}