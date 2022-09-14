package com.xihadislam.imagecmpo.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.xihadislam.imagecmpo.databinding.FragmentHomeBinding
import com.xihadislam.imagecmpo.utils.MyFileUtil
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import id.zelory.compressor.loadBitmap
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

class HomeFragment : Fragment() {
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    private var actualImage: File? = null
    private var compressedImage: File? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        homeViewModel.text.observe(viewLifecycleOwner) {
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actualImageView.setBackgroundColor(getRandomColor())
        clearImage()
        setupClickListener()

    }

    private fun clearImage() {
        binding.actualImageView.setBackgroundColor(getRandomColor())
        binding.compressedImageView.setImageDrawable(null)
        binding.compressedImageView.setBackgroundColor(getRandomColor())
        binding.compressedSizeTextView.text = "Size : -"
    }

    private fun getRandomColor() = Random().run {
        Color.argb(100, nextInt(256), nextInt(256), nextInt(256))
    }

    private fun setupClickListener() {
        binding.chooseImageButton.setOnClickListener { chooseImage() }
        binding.compressImageButton.setOnClickListener { compressImage() }
        binding.customCompressImageButton.setOnClickListener { customCompressImage() }
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            if (data == null) {
                showError("Failed to open picture!")
                return
            }
            try {
                actualImage = MyFileUtil.from(context, data.data)?.also {
                    binding.actualImageView.setImageBitmap(loadBitmap(it))
                    binding.actualSizeTextView.text =
                        String.format("Size : %s", getReadableFileSize(it.length()))
                    clearImage()
                }
            } catch (e: IOException) {
                showError("Failed to read picture data!")
                e.printStackTrace()
            }
        }
    }


    private fun compressImage() {
        actualImage?.let { imageFile ->
            lifecycleScope.launch {
                // Default compression
                compressedImage = context?.let { Compressor.compress(it, imageFile) }
                setCompressedImage()
            }
        } ?: showError("Please choose an image!")
    }

    private fun customCompressImage() {
        actualImage?.let { imageFile ->
            lifecycleScope.launch {
                // Default compression with custom destination file
                /*compressedImage = Compressor.compress(this@MainActivity, imageFile) {
                    default()
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.also {
                        val file = File("${it.absolutePath}${File.separator}my_image.${imageFile.extension}")
                        destination(file)
                    }
                }*/

                // Full custom
                compressedImage = context?.let {
                    Compressor.compress(it, imageFile) {
                        resolution(1280, 720)
                        quality(80)
                        format(Bitmap.CompressFormat.WEBP)
                        size(2_097_152) // 2 MB
                    }
                }
                setCompressedImage()
            }
        } ?: showError("Please choose an image!")
    }


    private fun setCompressedImage() {
        compressedImage?.let {
            binding.compressedImageView.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
            binding.compressedSizeTextView.text =
                String.format("Size : %s", getReadableFileSize(it.length()))
            Toast.makeText(context, "Compressed image save in " + it.path, Toast.LENGTH_LONG).show()
            Log.d("Compressor", "Compressed image save in " + it.path)
        }
    }

    private fun showError(errorMessage: String) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun getReadableFileSize(size: Long): String {
        if (size <= 0) {
            return "0"
        }
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}