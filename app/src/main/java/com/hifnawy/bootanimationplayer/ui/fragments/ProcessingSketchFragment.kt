package com.hifnawy.bootanimationplayer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hifnawy.bootanimationplayer.Sketch
import com.hifnawy.bootanimationplayer.databinding.FragmentProcessingSketchBinding
import processing.android.PFragment
import processing.core.PApplet

/**
 * [ProcessingSketchFragment] Fragment houses the skeleton for the
 * processing sketch.
 */
class ProcessingSketchFragment : Fragment() {
    private var binding: FragmentProcessingSketchBinding? = null
    private lateinit var sketch: PApplet

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (binding == null) {
            // Inflate the layout for this fragment
            binding = FragmentProcessingSketchBinding.inflate(inflater, container, false)
        }

        val file = ProcessingSketchFragmentArgs.fromBundle(requireArguments()).file
        val frame = binding!!.sketchContainer
        sketch = Sketch(this, file)
        val pFragment = PFragment(sketch)
        pFragment.setView(frame, activity)

        (activity as AppCompatActivity).supportActionBar?.title = file.name

        return binding!!.root
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        if (::sketch.isInitialized) {
            sketch.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}