package com.app.appelnfc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class EventAdapter(context: Context, val events: List<Event>) : ArrayAdapter<Event>(context, 0, events) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.event_item, parent, false)

        val event = events[position]

        val summaryTextView = view.findViewById<TextView>(R.id.summaryTextView)
        val locationTextView = view.findViewById<TextView>(R.id.locationTextView)
        val timeTextView = view.findViewById<TextView>(R.id.timeTextView)

        summaryTextView.text = event.summary
        locationTextView.text = event.location
        timeTextView.text = "${event.dtStart} - ${event.dtEnd}"

        return view
    }
}
