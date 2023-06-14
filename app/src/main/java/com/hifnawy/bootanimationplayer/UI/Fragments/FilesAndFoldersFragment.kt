package com.hifnawy.bootanimationplayer.UI.Fragments

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hifnawy.bootanimationplayer.Adapters.FilesAndFoldersAdapter
import com.hifnawy.bootanimationplayer.databinding.FragmentFilesAndFoldersBinding
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class FilesAndFoldersFragment : Fragment() {
    private companion object {
        private const val OPEN_ZIP_FILE_REQUEST_CODE: Int = 23081993
        private const val OPEN_DIRECTORY_REQUEST_CODE: Int = 23
        private const val READ_WRITE_EXT_STORAGE_REQUEST_CODE: Int = 8
        private const val ACCESS_ALL_FILES_REQUEST_CODE: Int = 1993
    }

    private var binding: FragmentFilesAndFoldersBinding? = null
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (binding == null) {
            // Inflate the layout for this fragment
            binding = FragmentFilesAndFoldersBinding.inflate(inflater, container, false)
        }
        navController = findNavController()

        with(binding!!) {
            foldersRecyclerView.visibility =
                if ((foldersRecyclerView.adapter != null) && (foldersRecyclerView.adapter!!.itemCount > 0)) View.VISIBLE else View.GONE

            addFilesAndFoldersFab.setOnClickListener {
                if (!arePermissionsGranted()) {
                    requestPermissions()
                } else {
                    // openFile(Uri.EMPTY)
                    openDirectory(Uri.EMPTY)
                }
            }
        }

        if (!arePermissionsGranted()) {
            requestPermissions()
        }

        return binding!!.root
    }

    private fun arePermissionsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) && ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                MaterialAlertDialogBuilder(requireContext()).setTitle("Manage All Files Access Required")
                    .setMessage("The Application needs permission to manage all files access to be able to operate on animation files. Grant access?")
                    .setPositiveButton("Ok") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.addCategory("android.intent.category.DEFAULT")
                            intent.data =
                                Uri.parse("package:${requireActivity().applicationContext.packageName}")
                            startActivityForResult(intent, ACCESS_ALL_FILES_REQUEST_CODE)
                        } catch (e: Exception) {
                            val intent = Intent()
                            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            startActivityForResult(intent, ACCESS_ALL_FILES_REQUEST_CODE)
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
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), READ_WRITE_EXT_STORAGE_REQUEST_CODE
                )
            } else {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), READ_WRITE_EXT_STORAGE_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        when (requestCode) {
            READ_WRITE_EXT_STORAGE_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // all permissions are granted
            } else {
                requestPermissions()
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        when (requestCode) {
            ACCESS_ALL_FILES_REQUEST_CODE -> if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && Environment.isExternalStorageManager()) {
                // all permissions are granted
            } else {
                Toast.makeText(activity, "Manage all files access rejected!", Toast.LENGTH_SHORT).show()
            }

            OPEN_ZIP_FILE_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                resultData?.data?.also { uri ->
                    val pathParts = uri.pathSegments[1].split(":")
                    val absolutePath =
                        if (pathParts[0].lowercase() == "primary") "${Environment.getExternalStorageDirectory().path}/${pathParts[1]}" else "/${
                            Environment.getExternalStorageDirectory().path.split(
                                "/"
                            )[1]
                        }/${pathParts.joinToString("/") { it }}"

                    // add to directories

                    with(navController) {
                        navigate(
                            directions = FilesAndFoldersFragmentDirections.actionToProcessingSketch(
                                zipFile = File(absolutePath)
                            )
                        )
                    }
                }
            }

            OPEN_DIRECTORY_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                resultData?.data?.also { uri -> readDirectoryContents(uri) }
            } else {
                Toast.makeText(activity, "no directory selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFile(pickerInitialUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.

                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
            startActivityForResult(intent, OPEN_ZIP_FILE_REQUEST_CODE)
        }
    }

    private fun openDirectory(pickerInitialUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Choose a directory using the system's file picker.
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                // Optionally, specify a URI for the directory that should be opened in
                // the system file picker when it loads.
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)

            }
            startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
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