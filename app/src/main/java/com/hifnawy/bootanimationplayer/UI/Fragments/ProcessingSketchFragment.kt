package com.hifnawy.bootanimationplayer.UI.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hifnawy.bootanimationplayer.R
import com.hifnawy.bootanimationplayer.Sketch
import com.hifnawy.bootanimationplayer.databinding.FragmentProcessingSketchBinding
import processing.android.PFragment
import processing.core.PApplet
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Use the [ProcessingSketchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProcessingSketchFragment : Fragment() {
    private lateinit var binding: FragmentProcessingSketchBinding
    private lateinit var sketch: PApplet

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProcessingSketchBinding.inflate(inflater, container, false)

        binding.button.setOnClickListener {
            findNavController().navigate(R.id.actionToFilesAndFolders)
        }

        val frame = binding.sketchContainer
        sketch = arguments!!.getString("zipFilePath")?.let { File(it) }?.let { Sketch(it) }!!
        val pFragment = PFragment(sketch)
        pFragment.setView(frame, activity)

        return binding.root
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        if (::sketch.isInitialized) {
            sketch.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


}