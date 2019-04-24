package com.github.varhastra.epicenter.main.feed

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.github.varhastra.epicenter.R
import com.github.varhastra.epicenter.domain.model.Event

class FeedAdapter(val context: Context) : RecyclerView.Adapter<FeedAdapter.EventHolder>() {

    var data: List<Event> = listOf()
        set (value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view_feed_event, parent, false)
        return EventHolder(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: EventHolder, position: Int) {
        holder.bind(data[position])
    }


    class EventHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.tv_item_feed_magnitude)
        lateinit var magnitudeTextView: TextView
        @BindView(R.id.tv_item_feed_title)
        lateinit var titleTextView: TextView

        init {
            ButterKnife.bind(this, itemView)
        }

        fun bind(event: Event) {
            magnitudeTextView.text = event.magnitude.toString() // TODO: implement formatting
            titleTextView.text = event.placeName
        }
    }
}