package com.codinlog.album.controller.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

import com.codinlog.album.R;
import com.codinlog.album.adapter.MainVPAdapter;
import com.codinlog.album.bean.FragmentBean;
import com.codinlog.album.bean.PhotoBean;
import com.codinlog.album.controller.BaseActivityController;
import com.codinlog.album.controller.Fragment.AlbumFragment;
import com.codinlog.album.controller.Fragment.PhotoFragment;
import com.codinlog.album.controller.Fragment.TimeFragment;
import com.codinlog.album.databinding.ActivityMainBinding;
import com.codinlog.album.entity.AlbumEntity;
import com.codinlog.album.entity.AlbumItemEntity;
import com.codinlog.album.listener.CommonListener;
import com.codinlog.album.listener.kotlin.AlbumItemListener;
import com.codinlog.album.model.AlbumViewModel;
import com.codinlog.album.model.MainViewModel;
import com.codinlog.album.model.PhotoViewModel;
import com.codinlog.album.model.TimeViewModel;
import com.codinlog.album.util.ClassifyUtil;
import com.codinlog.album.util.WorthStoreUtil;
import com.codinlog.album.widget.AlbumDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static androidx.fragment.app.FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;
import static com.codinlog.album.util.WorthStoreUtil.MODE.MODE_NORMAL;
import static com.codinlog.album.util.WorthStoreUtil.REQUEST_TAKE_PHOTO;
import static com.codinlog.album.util.WorthStoreUtil.albumPager;
import static com.codinlog.album.util.WorthStoreUtil.loaderManager_ID;
import static com.codinlog.album.util.WorthStoreUtil.photoPager;

public class MainActivity extends BaseActivityController<MainViewModel, ActivityMainBinding> {
    private ArrayList<FragmentBean> fragmentBeans;
    private MainVPAdapter mainVPAdapter;
    private String currentPhotoPath;
    private PopupMenu popupMenu;
    private Handler handler = new Handler();
    private static Object lock = new Object();

    @Override
    public void doInitViewData() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        binding.setData(viewModel);
        binding.bottomNavigation.setVisibility(View.GONE);
        viewModel.photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        viewModel.albumViewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        viewModel.timeViewModel = new ViewModelProvider(this).get(TimeViewModel.class);
        viewModel.photoViewModel.mainViewModel = viewModel;
        viewModel.albumViewModel.mainViewModel = viewModel;
        viewModel.timeViewModel.mainViewModel = viewModel;
        popupMenu = new PopupMenu(this, binding.btnMore);
        popupMenu.getMenuInflater().inflate(R.menu.top_menu, popupMenu.getMenu());
    }

    @Override
    public void doInitDisplayData() {
        mainVPAdapter = new MainVPAdapter(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        fragmentBeans = new ArrayList<>();
        fragmentBeans.add(new FragmentBean(PhotoFragment.newInstance(), getString(R.string.photo)));
        fragmentBeans.add(new FragmentBean(AlbumFragment.newInstance(), getString(R.string.album)));
        fragmentBeans.add(new FragmentBean(TimeFragment.newInstance(), getString(R.string.time)));
        viewModel.setFragments(fragmentBeans);
        binding.viewPager.setAdapter(mainVPAdapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
        loadPhotoData();
    }

    @Override
    public void doInitListener() {
        viewModel.getFragments().observe(this, fragmentBeans -> {
            if (mainVPAdapter == null)
                return;
            mainVPAdapter.setList(MainActivity.this.fragmentBeans);
            mainVPAdapter.notifyDataSetChanged();
        });
        viewModel.getPhotoBeans().observe(this, it -> {
            viewModel.photoViewModel.setClassifiedData(it);
        });
        viewModel.getMode().observe(this, mode -> {
            binding.viewPager.setCanScroll(mode == MODE_NORMAL);
            binding.bottomNavigation.getMenu().getItem(1).setChecked(true);
            binding.bottomNavigation.setVisibility(mode == MODE_NORMAL ? View.GONE : View.VISIBLE);
            binding.tabLayout.setVisibility(mode == MODE_NORMAL ? View.VISIBLE : View.INVISIBLE);
            binding.topBarSelectNotice.setVisibility(mode == MODE_NORMAL ? View.INVISIBLE : View.VISIBLE);
            binding.btnOperation.setImageDrawable(getDrawable(mode == MODE_NORMAL ? R.drawable.ic_camera_black_24dp : R.drawable.ic_delete_forever_black_24dp));
            viewModel.getIsSelectAll().setValue(false);
            viewModel.modeChanged();
        });
        viewModel.getTitle().observe(this, title -> {
            binding.topBarSelectNotice.setText(title);
        });
        viewModel.getIsSelectAll().observe(this, aBoolean -> binding.bottomNavigation.getMenu().getItem(2).setTitle(aBoolean ? getString(R.string.btn_all_cancel) : getString(R.string.btn_all)));
        viewModel.getCurrentPager().observe(this, integer -> {
            switch (integer) {
                case WorthStoreUtil.photoPager:
                    binding.bottomNavigation.getMenu().getItem(1).setIcon(R.drawable.ic_add_black_24dp);
                    binding.bottomNavigation.getMenu().getItem(1).setTitle(getString(R.string.addto_album));
                    break;
                case WorthStoreUtil.albumPager:
                    binding.bottomNavigation.getMenu().getItem(1).setIcon(R.drawable.ic_call_merge_black_24dp);
                    binding.bottomNavigation.getMenu().getItem(1).setTitle(getString(R.string.merge_album));
                    break;
                case WorthStoreUtil.timePager:
                    break;
            }
        });
        binding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                viewModel.setCurrentPager(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        binding.bottomNavigation.setOnNavigationItemSelectedListener(menuItem -> {
            if (viewModel.getCurrentPager().getValue() == photoPager) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_func:
                        if (viewModel.photoViewModel.getSelectedData().getValue().size() <= 0) {
                            Toast.makeText(MainActivity.this, getString(R.string.choice_item), Toast.LENGTH_SHORT).show();
                            break;
                        }
                        List<AlbumEntity> albumEntities = viewModel.albumViewModel.getDisplayData().getValue();
                        List<String> stringList = new ArrayList<>();
                        if (albumEntities != null) {
                            stringList = albumEntities.stream().map(AlbumEntity::getAlbumName).collect(Collectors.toList());
                        }
                        AlbumDialog albumDialog = new AlbumDialog(MainActivity.this)
                                .setBtnCancelListener(new CommonListener() {
                                    @Override
                                    public void handleEvent(Object o) {
                                        AlbumDialog dialog = (AlbumDialog) o;
                                        dialog.dismiss();
                                    }
                                })
                                .setBtnOkListener(o -> {
                                    AlbumDialog dialog = (AlbumDialog) o;
                                    String albumName = dialog.getInputContent().trim();
                                    if ("".equals(albumName))
                                        Toast.makeText(MainActivity.this, getString(R.string.enter_album_name), Toast.LENGTH_SHORT).show();
                                    else {
                                        AlbumEntity albumEntity = new AlbumEntity();
                                        albumEntity.setAlbumName(albumName);
                                        albumEntity.setDate(new Date());
                                        viewModel.photoViewModel.getSelectedData().getValue().forEach(
                                                it -> {
                                                    if (albumEntity.getPhotoBean() == null)
                                                        albumEntity.setPhotoBean(it);
                                                    if (it.getTokenDate() > albumEntity.getPhotoBean().getTokenDate())
                                                        albumEntity.setPhotoBean(it);
                                                }
                                        );
                                        viewModel.albumViewModel.queryAlbumById(albumEntity.hashCode(), o1 -> {
                                            if (o1 == null) {
                                                viewModel.albumViewModel.insertAlbumWithPhotoBeans(albumEntity, viewModel.photoViewModel.getSelectedData().getValue(), new CommonListener() {
                                                    @Override
                                                    public void handleEvent(Object o1) {
                                                        if (o1 != null && ((List<Long>) o1).size() > 0)
                                                            Toast.makeText(MainActivity.this, getString(R.string.addto_album_success), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                viewModel.albumViewModel.insertExistAlbumWithPhotoBeans(albumEntity, viewModel.photoViewModel.getSelectedData().getValue(), new CommonListener() {
                                                    @Override
                                                    public void handleEvent(Object o1) {
                                                        if (o1 != null && ((List<Long>) o1).size() > 0)
                                                            Toast.makeText(MainActivity.this, getString(R.string.addto_album_success), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                            viewModel.setMode(MODE_NORMAL);
                                            dialog.dismiss();
                                        });
                                    }
                                })
                                .setNoticeAdapterData(stringList)
                                .setTvTitle(getString(R.string.addto_album));
                        albumDialog.show();
                        break;
                    case R.id.menu_cancel:
                        viewModel.setMode(MODE_NORMAL);
                        break;
                    case R.id.menu_all:
                        viewModel.setIsSelectAllToOtherViewModel();
                        break;
                }
            } else if (viewModel.getCurrentPager().getValue() == albumPager) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_func:
                        break;
                    case R.id.menu_cancel:
                        viewModel.setMode(MODE_NORMAL);
                        break;
                    case R.id.menu_all:
                        viewModel.setIsSelectAllToOtherViewModel();
                        break;
                }
            }
            return true;
        });

        binding.btnOperation.setOnClickListener(v -> {
            switch (viewModel.getCurrentPager().getValue()) {
                case photoPager:
                    if (viewModel.getMode().getValue() == MODE_NORMAL) {
                        try {
                            dispatchTakePictureIntent();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.notice)
                                .setCancelable(false)
                                .setMessage(String.format(getString(R.string.delete_notice), viewModel.photoViewModel.getSelectedData().getValue().size()))
                                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                                    new deletePhotoBeansAsyncTask(o -> {
                                        String[] strings = (String[]) o;
                                        if (Arrays.stream(strings).anyMatch(Objects::nonNull)) {
                                            Toast.makeText(this, R.string.delete_not_all, Toast.LENGTH_LONG).show();
                                        }
                                        viewModel.setMode(MODE_NORMAL);
                                        dialog.dismiss();
                                    }).execute(viewModel.photoViewModel.getSelectedData().getValue());
                                })
                                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss()).show();
                    }
                    break;
            }
        });

        binding.btnMore.setOnClickListener(v -> popupMenu.show());
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.setting_1:
                    break;
            }
            return false;
        });
    }

    @Override
    protected void showPermissionDialog(final ArrayList<Integer> notAllowPermissions) {
        final Iterator<Integer> iterator = notAllowPermissions.iterator();
        if (iterator.hasNext()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.notice)
                    .setMessage(iterator.next())
                    .setPositiveButton(R.string.certain, (dialog, which) -> {
                        dialog.dismiss();
                        iterator.remove();
                        showPermissionDialog(notAllowPermissions);
                    }).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (viewModel.getMode().getValue() == WorthStoreUtil.MODE.MODE_SELECT)
            viewModel.setMode(MODE_NORMAL);
        else
            super.onBackPressed();
    }

    private void loadPhotoData() {
        LoaderManager loaderManager = LoaderManager.getInstance(this);
        loaderManager.initLoader(loaderManager_ID, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @NonNull
            @Override
            public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
                if (id == loaderManager_ID)
                    return new CursorLoader(MainActivity.this, WorthStoreUtil.imageUri, WorthStoreUtil.imageProjection, WorthStoreUtil.selectionRule, WorthStoreUtil.selectionArgs, WorthStoreUtil.orderRule);
                return null;
            }

            @Override
            public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
                List<PhotoBean> photoBeans = viewModel.getPhotoBeans().getValue();
                boolean isFirstScanning = photoBeans.isEmpty();
                boolean isReloadData = false;

                ClassifyUtil.removeDeletePhotoBeans(photoBeans, true);
                if (data != null && data.getCount() > 0) {
                    data.moveToFirst();
                    do {
                        boolean isContain = false;
                        String path = data.getString(data.getColumnIndexOrThrow(WorthStoreUtil.imageProjection[0]));
                        for (String str : WorthStoreUtil.disAllowScanning)
                            if (isContain = path.contains(str))
                                break;
                        File file = new File(path);
                        if (file.exists() && !isContain) {
                            String size = data.getString(data.getColumnIndexOrThrow(WorthStoreUtil.imageProjection[1]));
                            String id = data.getString(data.getColumnIndexOrThrow(WorthStoreUtil.imageProjection[2]));
                            String tokenData = data.getString(data.getColumnIndexOrThrow(WorthStoreUtil.imageProjection[3]));
                            String width = data.getString(data.getColumnIndexOrThrow(WorthStoreUtil.imageProjection[4]));
                            String height = data.getString(data.getColumnIndexOrThrow(WorthStoreUtil.imageProjection[5]));
                            if (isFirstScanning || (ClassifyUtil.isPhotoRepeat(photoBeans, path) == WorthStoreUtil.photoIsNew)) {
                                isReloadData = true;
                                PhotoBean photoBean = PhotoBean.newInstance();
                                photoBean.setPhotoPath(path);
                                photoBean.setPhotoSize(Long.parseLong(size));
                                photoBean.setPhotoId(Integer.parseInt(id));
                                photoBean.setTokenDate(Long.parseLong(tokenData));
                                photoBean.setDelete(false);
                                photoBean.setWidth(width == null ? 0 : Integer.parseInt(width));
                                photoBean.setHeight(height == null ? 0 : Integer.parseInt(height));
                                photoBeans.add(photoBean);
                            }
                        }
                    } while (data.moveToNext());
                    isReloadData = ClassifyUtil.removeDeletePhotoBeans(photoBeans, false) || isReloadData;
                    if (isReloadData) {
                        Collections.sort(photoBeans);
                        viewModel.setPhotoBeans(photoBeans);
                        new Thread(() -> {
                            updateAlbum();
                        }).start();
                    }
                }
            }

            @Override
            public void onLoaderReset(@NonNull Loader<Cursor> loader) {

            }
        });
    }

    private void updateAlbum() {
        synchronized (lock) {
            List<AlbumEntity> albumEntities = viewModel.albumViewModel.getAlbumDAO().queryAllAlbumWithList();
            if (albumEntities != null) {
                for (AlbumEntity albumEntity : albumEntities) {
                    List<AlbumItemEntity> albumItemEntities = viewModel.albumViewModel.getAlbumItemDAO().queryAllAlbumItem(albumEntity.getAlbumId());
                    if (albumItemEntities != null) {
                        Iterator<AlbumItemEntity> iterator = albumItemEntities.iterator();
                        boolean flag = true;
                        while (iterator.hasNext()) {
                            AlbumItemEntity albumItemEntity = iterator.next();
                            File file = new File(albumItemEntity.getPhotoBean().getPhotoPath());
                            if (file.exists()) {
                                if (flag) {
                                    albumEntity.setPhotoBean(albumItemEntity.getPhotoBean());
                                    flag = false;
                                }
                                iterator.remove();
                            }
                        }
                        if (albumItemEntities.size() > 0) {
                            viewModel.albumViewModel.deleteAlbumItem(albumItemEntities.toArray(new AlbumItemEntity[albumItemEntities.size()]));
                            viewModel.albumViewModel.updateAlbum(albumEntity);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(currentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }
    }


    private void dispatchTakePictureIntent() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.codinlog.album.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory() + File.separator + "MyAlbum/Pictures");
        if (!storageDir.exists())
            storageDir.mkdirs();
        File photoFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        currentPhotoPath = photoFile.getAbsolutePath();
        return photoFile;
    }


    class deletePhotoBeansAsyncTask extends AsyncTask<List<PhotoBean>, Integer, String[]> {
        private CommonListener commonListener;

        public deletePhotoBeansAsyncTask(CommonListener commonListener) {
            this.commonListener = commonListener;
        }

        @Override
        protected String[] doInBackground(List<PhotoBean>... photoBeans) {
            String[] filePaths = photoBeans[0].stream().map(PhotoBean::getPhotoPath).collect(Collectors.toList()).toArray(new String[photoBeans[0].size()]);
            return deletePhotoBeans(filePaths);
        }

        @Override
        protected void onPostExecute(String[] strings) {
            commonListener.handleEvent(strings);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    private String[] deletePhotoBeans(String... filePaths) {
        for (int i = 0; i < filePaths.length; i++) {
            File file = new File(filePaths[i]);
            if (file.exists() && file.isFile() && file.delete()) {
                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media.DATA + "=\"" + filePaths[i] + "\"", null);
                filePaths[i] = null;
            }
        }
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(new File(Environment.getExternalStorageState()));
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        return filePaths;
    }
}
