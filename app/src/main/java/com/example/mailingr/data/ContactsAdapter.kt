package com.example.mailingr.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mailingr.R

class ContactAdapter(private val contacts: List<Contact>) :
    RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactName: TextView = itemView.findViewById(R.id.contactName)
        private val contactPhone: TextView = itemView.findViewById(R.id.contactPhone)
        private val contactSendDate: TextView = itemView.findViewById(R.id.contactSendDate)  // Add this line

        fun bind(contact: Contact) {
            contactName.text = contact.name
            contactPhone.text = contact.phone
            contactSendDate.text = contact.sendDate ?: "No date"  // Add this line
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size
}
