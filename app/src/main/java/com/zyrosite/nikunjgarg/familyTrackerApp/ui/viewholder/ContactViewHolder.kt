package com.zyrosite.nikunjgarg.familyTrackerApp.ui.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.zyrosite.nikunjgarg.familyTrackerApp.data.model.UserContact
import com.zyrosite.nikunjgarg.familyTrackerApp.databinding.ContactTicketBinding

class ContactViewHolder(
    private val binding: ContactTicketBinding,
    onItemClicked: (Int) -> Unit
) :
    RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            onItemClicked(bindingAdapterPosition)
        }
    }
    fun bind(userContact: UserContact) {
        binding.tvName.text = userContact.name
        binding.tvPhoneNo.text = userContact.phoneNumber
    }
}