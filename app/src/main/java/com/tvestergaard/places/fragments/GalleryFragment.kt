package com.tvestergaard.places.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.tvestergaard.places.R
import kotlinx.android.synthetic.main.fragment_image.view.*
import org.jetbrains.anko.noButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.yesButton
import java.io.File
import java.io.IOException


class GalleryFragment : Fragment(), GalleryAdapterListener {


    private var imageDirectory: File? = null
    private var columnCount = 2
    private lateinit var adapter: GalleryAdapter
    private val images: MutableList<Image> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_image_list, container, false)
        if (view is RecyclerView) {
            this.adapter = GalleryAdapter(images, this)
            view.adapter = this.adapter
            view.layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
        }

        if (imageDirectory != null)
            readThumbnails(imageDirectory!!)
                .toTypedArray()
                .forEach(this::addImage)

        return view
    }

    /**
     * Returns the thumbnails within the provided directory.
     */
    private fun readThumbnails(directory: File): List<File> {
        if (!directory.exists() || !directory.isDirectory)
            return ArrayList(0)

        return directory.listFiles().filter { it.name.contains("thumb_") }
    }

    /**
     * Adds a new image to the gallery on screen.
     */
    fun addImage(image: File) {
        this.images.add(Image(image))
        this.adapter.notifyDataSetChanged()
    }

    override fun onClick(item: Image) {
        // throw UnsupportedOperationException("not supported")
    }

    override fun onLongClick(item: Image) {
        alert("Are you sure you want to delete this image?", "Delete Image") {
            yesButton {
                try {
                    item.file.delete()
                    File(item.file.parent, item.file.name.removePrefix("thumb_")).delete()
                    images.remove(item)
                    adapter.notifyDataSetChanged()
                    toast("The image war successfully been deleted.")
                } catch (e: IOException) {
                    toast("The image could not be deleted.")
                }
            }
            noButton {}
        }.show()
    }

    data class Image(val file: File) {
        override fun equals(other: Any?): Boolean {
            if (other !is Image)
                return false

            return other.file.absolutePath == file.absolutePath
        }

        override fun toString(): String = file.absolutePath
        override fun hashCode(): Int = file.hashCode()
    }

    companion object {
        fun create(imageDirectory: File? = null): GalleryFragment {
            val fragment = GalleryFragment()
            fragment.imageDirectory = imageDirectory
            return fragment
        }
    }
}

interface GalleryAdapterListener {
    fun onClick(item: GalleryFragment.Image)
    fun onLongClick(item: GalleryFragment.Image)
}

private class GalleryAdapter(
    private val images: List<GalleryFragment.Image>,
    private val listener: GalleryAdapterListener?
) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val data = BitmapFactory.decodeStream(images[position].file.inputStream())
        holder.imageView.setImageBitmap(data)

        with(holder.view) {

            tag = images[position]

            setOnClickListener { v ->
                val item = v.tag as GalleryFragment.Image
                listener?.onClick(item)
            }

            setOnLongClickListener { v ->
                val item = v.tag as GalleryFragment.Image
                listener?.onLongClick(item)
                true
            }
        }
    }

    override fun getItemCount(): Int = images.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.image
    }
}