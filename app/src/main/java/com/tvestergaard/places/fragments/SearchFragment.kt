package com.tvestergaard.places.fragments

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Geocoder
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import com.tvestergaard.places.transport.BackendCommunicator
import kotlinx.android.synthetic.main.fragment_search.*
import com.tvestergaard.places.SearchDetailActivity
import com.tvestergaard.places.glide
import com.tvestergaard.places.runOnUiThread
import com.tvestergaard.places.transport.InPlace
import kotlinx.android.synthetic.main.fragment_search_item.view.*
import org.jetbrains.anko.*
import com.tvestergaard.places.R.*


class SearchFragment : Fragment(), AnkoLogger {

    private val backendCommunicator = BackendCommunicator()
    private val results = mutableListOf<InPlace>()
    private lateinit var adapter: SearchResultsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(layout.fragment_search, container, false)

    override fun onStart() {
        super.onStart()

        adapter = SearchResultsAdapter(results, activity)
        searchResults.layoutManager = getLayoutManager()
        searchResults.adapter = adapter

        searchInput.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(search: String?): Boolean {
                this@SearchFragment.searchFor(search ?: "")
                return true
            }

            override fun onQueryTextChange(p0: String?) = true
        })

    }

    private fun getLayoutManager(): LinearLayoutManager {
        return GridLayoutManager(
            context, when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> 2
                else -> 1
            }
        )
    }

    private fun searchFor(search: String) {
        doAsync {
            results.clear()
            results.addAll(backendCommunicator.search(search))
            runOnUiThread {
                adapter.notifyDataSetChanged()

                // collapse the SearchView keyboard
                searchInput.clearFocus()
                searchInput.onActionViewCollapsed()
                screen.requestFocus()
            }
        }
    }

    private class SearchResultsAdapter(val items: List<InPlace>, val context: Context) : Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(context).inflate(
                    layout.fragment_search_item,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            if (holder != null) {
                with(holder) {
                    val place = items[position]
                    title.text = place.title
                    location.text = reverseGeocode(place.latitude.toDouble(), place.longitude.toDouble())
                    poster.text = place.user.name
                    thumbnail.glide(place.pictures[0].thumbName)
                    container.setOnClickListener { this@SearchResultsAdapter.showDetail(place) }
                }
            }
        }

        private fun showDetail(place: InPlace) {
            val intent = Intent(context, SearchDetailActivity::class.java)
            intent.putExtra("place", place)
            context.startActivity(intent)
        }

        private fun reverseGeocode(latitude: Double, longitude: Double): String {
            val geoCoder = Geocoder(context)
            val addresses = geoCoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.size > 0) {
                val address = addresses[0]
                val sb = StringBuilder()
                for (i in 0 until address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(", ")
                }
                sb.append(address.locality).append(", ")
                sb.append(address.postalCode).append(", ")
                sb.append(address.countryName)
                return sb.toString()
            }

            return "Could not locate."
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.title
        val thumbnail: ImageView = view.thumbnail
        val poster: TextView = view.poster
        val location: TextView = view.location
        val container = view
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            SearchFragment().apply {
                arguments = Bundle()
            }
    }
}
