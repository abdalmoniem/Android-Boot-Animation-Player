package com.hifnawy.bootanimationplayer.ui.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hifnawy.bootanimationplayer.adapters.FilesAndFoldersAdapter
import com.hifnawy.bootanimationplayer.databinding.FragmentFilesAndFoldersBinding
import java.io.File

/**
 * [FilesAndFoldersFragment] Entry Fragment of the application.
 *
 * Selects and Shows list of boot animation files
 */
class FilesAndFoldersFragment : Fragment() {

    private var binding: FragmentFilesAndFoldersBinding? = null
    private lateinit var navController: NavController

    private val startActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::activityResults)

    private val requestPermissions =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            ::permissionRequestResults
        )

    @Suppress("unused")
    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument(), ::openDocumentResult)

    private val openDocumentTree =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree(), ::openDocumentTreeResult)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (binding == null) {
            // Inflate the layout for this fragment
            binding = FragmentFilesAndFoldersBinding.inflate(inflater, container, false)
        }
        navController = findNavController()

        if (shouldRequestPermissions()) {
            requestPermissions()
        }

        with(binding!!) {
            foldersRecyclerView.visibility =
                if ((foldersRecyclerView.adapter != null) && (foldersRecyclerView.adapter!!.itemCount > 0)) View.VISIBLE else View.GONE

            addFilesAndFoldersFab.setOnClickListener {
                if (shouldRequestPermissions()) {
                    requestPermissions()
                } else {
                    // openDocument.launch(arrayOf("application/zip"))
                    openDocumentTree.launch(Uri.EMPTY)
                }
            }
        }

        return binding!!.root
    }

    private fun shouldRequestPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) || ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                MaterialAlertDialogBuilder(requireContext()).setTitle("Manage All Files Access Required")
                    .setMessage("The Application needs permission to manage all files access to be able to operate on animation files. Grant access?")
                    .setPositiveButton("Ok") { _, _ ->
                        try {
                            startActivityResult.launch(
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).addCategory(
                                    "android.intent.category.DEFAULT"
                                )
                                    .setData(Uri.parse("package:${requireActivity().applicationContext.packageName}"))
                            )
                        } catch (e: Exception) {
                            startActivityResult.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    }.setNegativeButton(
                        "Cancel"
                    ) { _, _ ->
                        Toast.makeText(
                            activity, "Manage all files access rejected!", Toast.LENGTH_SHORT
                        ).show()
                    }.show()

            } else {
                // all permission are granted
            }
        } else {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(
                    activity,
                    "Storage permission required. Please allow this permission",
                    Toast.LENGTH_LONG
                ).show()

                requestPermissions.launch(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun permissionRequestResults(permissions: Map<String, @JvmSuppressWildcards Boolean>) {
        permissions.entries.forEach { permission ->
            Log.d("PERMISSIONS", "${permission.key} : ${permission.value}")
        }
    }

    private fun openDocumentResult(uri: Uri?) {
        if (uri != null) {
            val pathParts = uri.pathSegments[1].split(":")
            val absolutePath =
                if (pathParts[0].lowercase() == "primary") "${Environment.getExternalStorageDirectory().path}/${pathParts[1]}" else "/${
                    Environment.getExternalStorageDirectory().path.split(
                        "/"
                    )[1]
                }/${pathParts.joinToString("/") { it }}"

            navController.navigate(
                directions = FilesAndFoldersFragmentDirections.actionToProcessingSketch(
                    file = File(absolutePath)
                )
            )

        } else {
            Toast.makeText(activity, "no file selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDocumentTreeResult(uri: Uri?) {
        if (uri != null) {
            readDirectoryContents(uri)
        } else {
            Toast.makeText(activity, "no directory selected!", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun activityResults(result: ActivityResult) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // all permissions are granted
            } else {
                Toast.makeText(activity, "Manage all files access rejected!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun readDirectoryContents(path: Uri) {
        val pathParts = path.pathSegments[1].split(":")
        val absolutePath =
            if (pathParts[0].lowercase() == "primary") "${Environment.getExternalStorageDirectory().path}/${pathParts[1]}" else "/${
                Environment.getExternalStorageDirectory().path.split(
                    "/"
                )[1]
            }/${pathParts.joinToString("/") { it }}"

        val directory = File(absolutePath)

        if (directory.isDirectory) {
            val zipFiles: ArrayList<File> = ArrayList()

            directory.list()!!.forEach {
                val file = File("$absolutePath/$it")

                if (!file.isDirectory && (file.extension.lowercase() == "zip")) {
                    zipFiles.add(file)
                }
            }

            // populate RecyclerView with zipFiles data
            if (zipFiles.size > 0) {
                binding!!.noFilesOrFoldersSelectedTextView.visibility = View.GONE
                binding!!.foldersRecyclerView.visibility = View.VISIBLE
                binding!!.foldersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding!!.foldersRecyclerView.adapter =
                    FilesAndFoldersAdapter(requireContext(), zipFiles)
            }
        }
    }
}