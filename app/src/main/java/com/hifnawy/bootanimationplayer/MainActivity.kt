package com.hifnawy.bootanimationplayer

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import com.hifnawy.bootanimationplayer.databinding.ActivityMainBinding
import processing.android.CompatUtils
import processing.android.PFragment
import processing.core.PApplet
import java.io.File


class MainActivity : AppCompatActivity() {
    private val OPEN_DIRECTORY_REQUEST_CODE: Int = 0
    private val READ_EXT_STORAGE_REQUEST_CODE: Int = 100
    private val ACCESS_ALL_FILES_REQUEST_CODE: Int = 200

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sketch: PApplet

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        requestPermissions()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                    startActivityForResult(intent, ACCESS_ALL_FILES_REQUEST_CODE)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivityForResult(intent, ACCESS_ALL_FILES_REQUEST_CODE)
                }
            } else {
                openDirectory(Uri.EMPTY)
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                Toast.makeText(
                    this, "Storage permission required. Please allow this permission", Toast.LENGTH_LONG
                ).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_EXT_STORAGE_REQUEST_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_EXT_STORAGE_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        sketch.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXT_STORAGE_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openDirectory(Uri.EMPTY)
            } else {
                requestPermissions()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        when (requestCode) {
            ACCESS_ALL_FILES_REQUEST_CODE -> if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && Environment.isExternalStorageManager()) {
                openDirectory(Uri.EMPTY)
            } else {
                Toast.makeText(this, "Manage all files access rejected!", Toast.LENGTH_SHORT).show()
            }
            OPEN_DIRECTORY_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                resultData?.data?.also { uri -> readDirectoryContents(uri) }
            } else {
                Toast.makeText(this, "no directory selected!", Toast.LENGTH_SHORT).show()
            }
        }

        super.onActivityResult(requestCode, resultCode, resultData)
    }

    override fun onNewIntent(intent: Intent?) {
        sketch.onNewIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
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

            val frame = binding.sketchContainer
            sketch = Sketch(zipFiles)
            val pFragment = PFragment(sketch)
            pFragment.setView(frame, this)
        }
    }
}