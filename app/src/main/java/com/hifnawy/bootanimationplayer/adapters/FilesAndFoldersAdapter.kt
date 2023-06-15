package com.hifnawy.bootanimationplayer.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.hifnawy.bootanimationplayer.R
import com.hifnawy.bootanimationplayer.ui.fragments.FilesAndFoldersFragmentDirections
import com.hifnawy.bootanimationplayer.viewHolders.FolderListItemViewHolder
import java.io.File

class FilesAndFoldersAdapter
constructor(
    private val context: Context, folders: ArrayList<File>
) : RecyclerView.Adapter<FolderListItemViewHolder>() {
    private val folderFiles: ArrayList<File> = folders

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderListItemViewHolder {
        return FolderListItemViewHolder(
            LayoutInflater.from(context).inflate(R.layout.folder_list_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return folderFiles.size
    }

    override fun onBindViewHolder(holder: FolderListItemViewHolder, position: Int) {
        with(holder) {
            val file = folderFiles[position]
            folderName.text = file.name
            folderPath.text = file.absolutePath
            cardView.setOnClickListener {
                with(Navigation.findNavController(itemView)) {
                    navigate(
                        directions = FilesAndFoldersFragmentDirections.actionToProcessingSketch(
                            zipFile = file
                        )
                    )
                }
                Log.d("FILE", "clicked on ${file.name} stored at ${file.absoluteFile}")
            }
        }
    }
}