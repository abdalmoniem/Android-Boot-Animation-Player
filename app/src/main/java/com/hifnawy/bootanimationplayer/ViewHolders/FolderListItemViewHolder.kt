package com.hifnawy.bootanimationplayer.ViewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.hifnawy.bootanimationplayer.databinding.FolderListItemBinding

class FolderListItemViewHolder(itemView: View) : ViewHolder(itemView) {
    private val binding = FolderListItemBinding.bind(itemView)

    val folderIcon = binding.folderIcon
    val folderName = binding.folderName
    val folderPath = binding.folderPath
    val cardView = binding.cardView
    val arrowDownIcon = binding.arrowDown
}