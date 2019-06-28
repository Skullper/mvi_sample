package com.example.mobiusvk.mvi

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mobiusvk.R
import kotlinx.android.synthetic.main.item_news.view.*

fun ViewGroup.inflate(@LayoutRes resource: Int): View {
    return LayoutInflater.from(this.context).inflate(resource, this, false)
}

class FeedAdapter : RecyclerView.Adapter<FeedAdapter.FeedItemViewHolder>() {

    var items = listOf<FeedItem>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FeedItemViewHolder(parent.inflate(R.layout.item_news))

    override fun onBindViewHolder(holder: FeedItemViewHolder, position: Int) {
        with(holder.itemView) {
            tv_news_item.text = items[position].title
            tv_news_item_text.text = items[position].text
        }
    }

    override fun getItemCount() = items.size

    class FeedItemViewHolder(view: View) : RecyclerView.ViewHolder(view)
}