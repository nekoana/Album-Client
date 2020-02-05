package com.codinlog.album.controller.Fragment;

import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.codinlog.album.R;
import com.codinlog.album.adapter.PhotoRVAdpater;
import com.codinlog.album.bean.PhotoBean;
import com.codinlog.album.controller.Activity.PhotoPreviewActivity;
import com.codinlog.album.controller.BaseFragmentController;
import com.codinlog.album.databinding.PhotoFragmentBinding;
import com.codinlog.album.listener.PhotoItemListener;
import com.codinlog.album.listener.PhotoGroupListener;
import com.codinlog.album.model.PhotoViewModel;
import com.codinlog.album.util.WorthStoreUtil;

public class PhotoFragment extends BaseFragmentController<PhotoViewModel> {
    private PhotoRVAdpater photoRVAdpater;
    private PhotoFragmentBinding photoFragmentBinding;

    public static PhotoFragment newInstance() {
        return new PhotoFragment();
    }


    @Override
    protected int getLayoutId() {
        return R.layout.photo_fragment;
    }

    @Override
    protected void doInitView() {
        viewModel = new ViewModelProvider(getActivity()).get(PhotoViewModel.class);
        photoFragmentBinding = (PhotoFragmentBinding) super.binding;
    }

    @Override
    protected void doInitListener() {
        viewModel.getClassifiedResListMutableLiveData().observe(getViewLifecycleOwner(), objects -> photoRVAdpater.setData(objects));
        viewModel.getSelectedMutableLiveData().observe(getViewLifecycleOwner(), integers -> photoRVAdpater.notifyChange(null, true));
        viewModel.mainViewModel.getModeMutableLiveData().observe(getViewLifecycleOwner(), mode -> {
            if (viewModel.mainViewModel.getModeMutableLiveData().getValue() == WorthStoreUtil.MODE.MODE_NORMAL)
                viewModel.modeChangeToNormal();
            photoRVAdpater.setMode(mode);
        });
        viewModel.getIsSelectedAllGroupMutableLiveData().observe(getViewLifecycleOwner(), aBoolean -> photoRVAdpater.notifyChange(null, true));
    }

    @Override
    protected void doInitData() {
        photoRVAdpater = new PhotoRVAdpater(new PhotoItemListener() {
            @Override
            public void handleEvent(int position) {
                if (viewModel.mainViewModel.getModeMutableLiveData().getValue() != WorthStoreUtil.MODE.MODE_SELECT)
                    viewModel.mainViewModel.setModeMutableLiveData(WorthStoreUtil.MODE.MODE_SELECT);
                selectPhotoChanged(position, false, false, false);
            }
        }, new PhotoItemListener() {
            @Override
            public void handleEvent(int position) {
                if (viewModel.mainViewModel.getModeMutableLiveData().getValue() == WorthStoreUtil.MODE.MODE_SELECT) {
                    selectPhotoChanged(position, false, false, false);
                } else {
                    Intent intent = new Intent(getContext(), PhotoPreviewActivity.class);
                    PhotoBean photoBean = (PhotoBean) viewModel.getClassifiedResListMutableLiveData().getValue().get(position);
                    int currentPosition = 0;
                    for (PhotoBean p : viewModel.mainViewModel.getClassifiedPhotoBeanMutableLiveData().getValue()) {
                        if (p.getPhotoId() == photoBean.getPhotoId()) {
                            intent.putExtra("currentPosition", currentPosition);
                            break;
                        }
                        currentPosition++;
                    }
                    startActivity(intent);
                }
            }
        }, new PhotoItemListener() {
            @Override
            public void handleEvent(int position) {
                selectPhotoChanged(position, false, false, false);
            }
        }, new PhotoGroupListener() {
            @Override
            public void handleEvent(int position, boolean isChecked) {
                viewModel.setIsSelectedGroupAllMutableLiveData(isChecked);
                selectPhotoChanged(position, true, true, false);
            }
        }, new PhotoGroupListener() {
            @Override
            public void handleEvent(int position, boolean isChecked) {
                if (viewModel.mainViewModel.getModeMutableLiveData().getValue() != WorthStoreUtil.MODE.MODE_SELECT)
                    viewModel.mainViewModel.setModeMutableLiveData(WorthStoreUtil.MODE.MODE_SELECT);
                viewModel.setIsSelectedGroupAllMutableLiveData(!isChecked);
                selectPhotoChanged(position, true, true, false);
            }
        });
        photoFragmentBinding.rv.setLayoutManager(new GridLayoutManager(getContext(), WorthStoreUtil.thumbnailPhotoNum));
        photoFragmentBinding.rv.setAdapter(photoRVAdpater);
    }

    private void selectPhotoChanged(int position, boolean isRepeat, boolean isGroupAll, boolean isAllGroup) {
        viewModel.changeSelectMutableLiveData(position, isRepeat, isGroupAll, isAllGroup);
    }
}
