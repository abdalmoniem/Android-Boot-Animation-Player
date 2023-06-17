package com.hifnawy.bootanimationplayer.adapters

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.hifnawy.bootanimationplayer.R
import com.hifnawy.bootanimationplayer.databinding.LoadingDialogBinding
import com.hifnawy.bootanimationplayer.ui.fragments.FilesAndFoldersFragmentDirections
import com.hifnawy.bootanimationplayer.viewHolders.FolderListItemViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
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
        val coroutineScope = holder.itemView.findViewTreeLifecycleOwner()?.lifecycleScope
            ?: CoroutineScope(Dispatchers.IO)

        with(holder) {
            val file = folderFiles[position]
            folderName.text = file.name
            folderPath.text = file.absolutePath
            cardView.setOnClickListener {

                val dialogBinding: LoadingDialogBinding = LoadingDialogBinding.inflate(
                    LayoutInflater.from(context), null, false
                )

                val dialog = Dialog(context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(false)
                dialog.setContentView(dialogBinding.root)
                dialogBinding.message.text = "Loading file $file..."
                dialog.show()

                coroutineScope.launch {
                    ZipFile(file).extractAll(file.path.replace(".zip", ""))

                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Navigation.findNavController(itemView).navigate(
                            directions = FilesAndFoldersFragmentDirections.actionToProcessingSketch(
                                animationFolder = File(file.path.replace(".zip", ""))
                            )
                        )
                    }
                }
                Log.d("FILE", "clicked on ${file.name} stored at ${file.absoluteFile}")
            }
        }
    }
}