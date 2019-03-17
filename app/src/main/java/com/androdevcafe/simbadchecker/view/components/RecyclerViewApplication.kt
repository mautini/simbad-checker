package com.androdevcafe.simbadchecker.view.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androdevcafe.simbadchecker.R
import com.androdevcafe.simbadchecker.model.Application
import kotlinx.android.synthetic.main.application_item.view.*

class RecyclerViewApplication(private val applications: List<Application>,
                              private val clickSettings: (Application) -> Unit)
    : RecyclerView.Adapter<RecyclerViewApplication.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.name
        val packageName: TextView = view.packageName
        val icon: ImageView = view.icon
        val settings: Button = view.settings
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.application_item, parent, false))
    }

    override fun getItemCount() = applications.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val application = applications[position]
        holder.name.text = application.name
        holder.packageName.text = application.packageName
        holder.icon.setImageDrawable(application.icon)
        holder.settings.setOnClickListener { clickSettings(application) }
    }
}