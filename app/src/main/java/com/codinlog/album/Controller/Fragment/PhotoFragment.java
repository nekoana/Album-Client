package com.codinlog.album.Controller.Fragment;

import android.util.Log;
import android.widget.CheckBox;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import androidx.recyclerview.widget.GridLayoutManager;

import com.codinlog.album.Controller.BaseFragmentController;
import com.codinlog.album.R;
import com.codinlog.album.adapter.PhotoRecyclerViewAdpater;
import com.codinlog.album.bean.ImageBean;
import com.codinlog.album.databinding.PhotoFragmentBinding;
import com.codinlog.album.listener.PhotoItemCheckBoxListener;
import com.codinlog.album.listener.PhotoItemOnClickListener;
import com.codinlog.album.listener.PhotoItemOnLongClickListenser;
import com.codinlog.album.model.PhotoViewModel;
import com.codinlog.album.util.WorthStoreUtil;

import java.util.ArrayList;

public class PhotoFragment extends BaseFragmentController<PhotoViewModel> {
    private PhotoRecyclerViewAdpater photoRecyclerViewAdpater;
    PhotoFragmentBinding photoFragmentBinding;

    public static PhotoFragment newInstance() {
        return new PhotoFragment();
    }


    @Override
    protected int getLayoutId() {
        return R.layout.photo_fragment;
    }

    @Override
    protected void doInitListener() {
        viewModel.getObjectMutableLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<Object>>() {
            @Override
            public void onChanged(ArrayList<Object> objects) {
                photoRecyclerViewAdpater.setData(objects);
            }
        });

        viewModel.getModeMutableLiveData().observe(getViewLifecycleOwner(), new Observer<WorthStoreUtil.MODE>() {
            @Override
            public void onChanged(WorthStoreUtil.MODE mode) {
                photoRecyclerViewAdpater.setMode(mode);
            }
        });
    }

    @Override
    protected void doInitView() {
        viewModel = ViewModelProviders.of(getActivity()).get(PhotoViewModel.class);
        photoRecyclerViewAdpater = new PhotoRecyclerViewAdpater(new PhotoItemOnLongClickListenser() {
            @Override
            public void handleEvent(int position) {
                if (viewModel.getModeMutableLiveData().getValue() == WorthStoreUtil.MODE.MODE_SELECT)
                    return;
                else {
                    viewModel.setModeMutableLiveData(WorthStoreUtil.MODE.MODE_SELECT);
                    ArrayList<Object> objectArrayList = viewModel.getObjectMutableLiveData().getValue();
                    Object o = objectArrayList.get(position);
                    if (o instanceof ImageBean) {
                        ImageBean imageBean = (ImageBean) o;
                        imageBean.setSelected(!imageBean.isSelected());
                        objectArrayList.set(position, imageBean);
                        photoRecyclerViewAdpater.notifyItemChanged(position,"payload");
                    }
                }
            }
        }, new PhotoItemOnClickListener() {
            @Override
            public void handleEvent(int position) {
                ArrayList<Object> objectArrayList = viewModel.getObjectMutableLiveData().getValue();
                Object o = objectArrayList.get(position);
                if (o instanceof ImageBean) {
                    ImageBean imageBean = (ImageBean) o;
                    imageBean.setSelected(!imageBean.isSelected());
                    objectArrayList.set(position, imageBean);
                    photoRecyclerViewAdpater.notifyItemChanged(position,"payload");
                }
            }
        }, new PhotoItemCheckBoxListener() {
            @Override
            public void handleEvent(int position) {
                ArrayList<Object> objectArrayList = viewModel.getObjectMutableLiveData().getValue();
                Object o = objectArrayList.get(position);
                if (o instanceof ImageBean) {
                    ImageBean imageBean = (ImageBean) o;
                    imageBean.setSelected(!imageBean.isSelected());
                    objectArrayList.set(position, imageBean);
                    photoRecyclerViewAdpater.notifyItemChanged(position,"payload");
                }
            }
        });
        photoFragmentBinding = (PhotoFragmentBinding) binding;
    }

    @Override
    protected void doInitData() {
        photoFragmentBinding.rv.setLayoutManager(new GridLayoutManager(getContext(), WorthStoreUtil.thumbnailImageNum));
        photoFragmentBinding.rv.setAdapter(photoRecyclerViewAdpater);
    }
}
