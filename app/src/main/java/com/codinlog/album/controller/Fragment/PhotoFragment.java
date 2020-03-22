package com.codinlog.album.controller.Fragment;

import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.codinlog.album.R;
import com.codinlog.album.adapter.PhotoRVAdapter;
import com.codinlog.album.bean.GroupBean;
import com.codinlog.album.bean.PhotoBean;
import com.codinlog.album.controller.Activity.PhotoPreviewActivity;
import com.codinlog.album.controller.Activity.kotlin.AlbumPreviewActivity;
import com.codinlog.album.controller.BaseFragmentController;
import com.codinlog.album.databinding.PhotoFragmentBinding;
import com.codinlog.album.listener.PhotoGroupListener;
import com.codinlog.album.model.PhotoViewModel;
import com.codinlog.album.util.DataStoreUtil;
import com.codinlog.album.util.WorthStoreUtil;

public class PhotoFragment extends BaseFragmentController<PhotoViewModel, PhotoFragmentBinding> {
    private PhotoRVAdapter photoRVAdapter;

    public static PhotoFragment newInstance() {
        return new PhotoFragment();
    }


    @Override
    protected int getLayoutId() {
        return R.layout.photo_fragment;
    }

    @Override
    public void doInitViewData() {
        viewModel = new ViewModelProvider(getActivity()).get(PhotoViewModel.class);
    }

    @Override
    public void doInitListener() {
        viewModel.getDisplayData().observe(getViewLifecycleOwner(), v -> photoRVAdapter.setData(v));
        viewModel.getClassifiedDisplayDataMap().observe(getViewLifecycleOwner(), v -> viewModel.setDisplayData());
        viewModel.getSelectedData().observe(getViewLifecycleOwner(), v -> {
            photoRVAdapter.notifySelectChanged();
            viewModel.mainViewModel.setTitle();
        });
        viewModel.getMode().observe(getViewLifecycleOwner(), mode -> {
            if (photoRVAdapter != null)
                photoRVAdapter.setMode(mode);
            if (mode == WorthStoreUtil.MODE.MODE_NORMAL)
                viewModel.resetDisplayData();
        });
    }

    @Override
    public void doInitDisplayData() {
        photoRVAdapter = new PhotoRVAdapter(position -> {
            if (viewModel.getMode().getValue() != WorthStoreUtil.MODE.MODE_SELECT) {
                viewModel.mainViewModel.setMode(WorthStoreUtil.MODE.MODE_SELECT);
            }
            selectPhotoChanged((int) position, false);
        }, position -> {
            if (viewModel.getMode().getValue() == WorthStoreUtil.MODE.MODE_SELECT) {
                selectPhotoChanged((int) position, false);
            } else {
                Object o = viewModel.getDisplayData().getValue().get((int) position);
                if (o instanceof PhotoBean) {
                    Intent intent = new Intent(getContext(), PhotoPreviewActivity.class);
                    intent.putExtra("photoBean", (PhotoBean) o);
                    startActivity(intent);
                }
            }
        }, position -> selectPhotoChanged((int) position, false), new PhotoGroupListener() {
            @Override
            public void handleEvent(Object o) {
            }

            @Override
            public void handleEvent(int position, boolean isChecked) {
                selectPhotoChanged(position, isChecked);
            }
        }, new PhotoGroupListener() {
            @Override
            public void handleEvent(Object o) {
            }

            @Override
            public void handleEvent(int position, boolean isChecked) {
                if (viewModel.getMode().getValue() != WorthStoreUtil.MODE.MODE_SELECT)
                    viewModel.mainViewModel.setMode(WorthStoreUtil.MODE.MODE_SELECT);
                selectPhotoChanged(position, !isChecked);
            }
        }, position -> {
            Object o = viewModel.getDisplayData().getValue().get((int) position);
            if (o instanceof GroupBean) {
                GroupBean groupBean = (GroupBean) o;
                DataStoreUtil.getInstance().setDisplayData(viewModel.getClassifiedDisplayDataMap().getValue().get(groupBean));
                Intent intent = new Intent(getContext(), AlbumPreviewActivity.class);
                intent.putExtra("from", "photo");
                intent.putExtra("fromValue", groupBean.getGroupId());
                startActivity(intent);
            }
        });
        binding.rv.setLayoutManager(new GridLayoutManager(getContext(), WorthStoreUtil.thumbnailPhotoNum));
        binding.rv.setAdapter(photoRVAdapter);
    }

    private void selectPhotoChanged(int position, boolean isGroupAll) {
        viewModel.changeSelectLiveData(position, isGroupAll);
    }
}
