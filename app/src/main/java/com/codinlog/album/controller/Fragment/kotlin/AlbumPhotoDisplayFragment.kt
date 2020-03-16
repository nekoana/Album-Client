package com.codinlog.album.controller.Fragment.kotlin

import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.codinlog.album.R
import com.codinlog.album.adapter.kotlin.AlbumDisplayRVAdapter
import com.codinlog.album.controller.Activity.PhotoPreviewActivity
import com.codinlog.album.controller.BaseFragmentController
import com.codinlog.album.databinding.FragmentAlbumDisplayBinding
import com.codinlog.album.listener.CommonListener
import com.codinlog.album.model.kotlin.AlbumDisplayViewModel
import com.codinlog.album.util.DataStoreUtil
import com.codinlog.album.util.WorthStoreUtil


class AlbumPhotoDisplayFragment : BaseFragmentController<AlbumDisplayViewModel, FragmentAlbumDisplayBinding>() {
    private lateinit var albumDisplayRVAdapter: AlbumDisplayRVAdapter
    override fun getLayoutId(): Int {
        return R.layout.fragment_album_display
    }

    override fun doInitViewData() {
        viewModel = activity?.let { ViewModelProvider(it).get(AlbumDisplayViewModel::class.java) }
    }

    override fun doInitListener() {
        viewModel.displayData.observe(viewLifecycleOwner, Observer {
            albumDisplayRVAdapter.photoBeans = it
        })
    }

    override fun doInitDisplayData() {
        albumDisplayRVAdapter = AlbumDisplayRVAdapter(CommonListener {
            when (viewModel.albumPreviewViewModel?.currentModelMutableLiveData?.value) {
                WorthStoreUtil.MODE.MODE_NORMAL -> {
                    val intent = Intent(context, PhotoPreviewActivity::class.java)
                    intent.putExtra("photoBean", viewModel.displayData.value?.get(it as Int))
                    DataStoreUtil.getInstance().allDisplayData = viewModel.displayData.value
                    startActivity(intent)
                }
                WorthStoreUtil.MODE.MODE_SELECT -> {

                }
            }
        }, CommonListener {

        })
        binding.rv.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rv.adapter = albumDisplayRVAdapter
    }

    companion object {
        @JvmStatic
        fun newInstance() = AlbumPhotoDisplayFragment()
    }
}
