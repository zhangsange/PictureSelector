package com.luck.picture.lib;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.permissions.RxPermissions;
import com.luck.picture.lib.photoview.OnViewTapListener;
import com.luck.picture.lib.photoview.PhotoView;
import com.luck.picture.lib.rxbus2.RxBus;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.luck.picture.lib.tools.ToastManage;
import com.luck.picture.lib.widget.PreviewViewPager;
import com.luck.picture.lib.widget.longimage.ImageSource;
import com.luck.picture.lib.widget.longimage.ImageViewState;
import com.luck.picture.lib.widget.longimage.SubsamplingScaleImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.ui
 * email：邮箱->893855882@qq.com
 * data：17/01/18
 */
public class PictureExternalPreviewActivity extends PictureBaseActivity implements View.OnClickListener {
    public static final String EXTRA_OPEN_TYPE = "EXTRA_OPEN_TYPE";
    public static final int OPEN_TYPE_NONE = 0;
    public static final int OPEN_TYPE_SAVE = 1;
    public static final int OPEN_TYPE_DELETE = 2;
    private ImageView left_back;
    protected ImageButton ic_right;
    private TextView tv_title;
    private PreviewViewPager viewPager;
    private List<LocalMedia> images = new ArrayList<>();
    private int position = 0;

    /**
     * 打开预览的方式（右上角的按钮）
     * 0：无
     * 1：保存
     * 2：删除
     */
    private int type = OPEN_TYPE_NONE;

    /**
     * 图片保存的路径
     */
    private String directory_path;
    private SimpleFragmentAdapter adapter;
    private LayoutInflater inflater;
    private RxPermissions rxPermissions;
    private loadDataThread loadDataThread;
    /**
     * 是否显示dialog
     */
    private boolean shouldShowDialog = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_activity_external_preview);
        inflater = LayoutInflater.from(this);
        tv_title = (TextView) findViewById(R.id.picture_title);
        left_back = (ImageView) findViewById(R.id.left_back);
        ic_right = (ImageButton) findViewById(R.id.right_icon);
        viewPager = (PreviewViewPager) findViewById(R.id.preview_pager);
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        type = getIntent().getIntExtra(EXTRA_OPEN_TYPE,OPEN_TYPE_NONE);
        directory_path = getIntent().getStringExtra(PictureConfig.DIRECTORY_PATH);
        shouldShowDialog = getIntent().getBooleanExtra("shouldShowDialog",true);
        images = (List<LocalMedia>) getIntent().getSerializableExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST);

        if (type==0) {
            ic_right.setVisibility(View.GONE);
        }else if(type ==1){
            ic_right.setVisibility(View.VISIBLE);
            ic_right.setImageResource(R.drawable.download_icon);
            ic_right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String curPath = images.get(position).getPath();
                    savePic(curPath);
                }
            });
        }else if(type ==2){
            ic_right.setVisibility(View.VISIBLE);
            ic_right.setImageResource(R.drawable.icon_del);
            ic_right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new MaterialDialog.Builder(PictureExternalPreviewActivity.this)
                            .title("提示")
                            .positiveText("删除")
                            .negativeText("取消")
                            .content("确定删除此图片吗？")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                                    images.remove(position);
//                                    adapter.notifyDataSetChanged();
                                    position = viewPager.getCurrentItem();
                                    images.remove(position);
                                    System.out.println("image.size()===="+images.size());
                                    System.out.println("position===="+position);
                                    if (images.size() > 0) {
                                        if (images.size() == 1) {
                                            tv_title.setText(1 + "/" + 1);
                                        } else {
                                            tv_title.setText((position + 1) + "/" + images.size());
                                        }
//                                        listViews.remove(position);
                                        viewPager.setAdapter(adapter);
                                        viewPager.setCurrentItem(position > 0 ? position : 0);
                                    } else {
                                        onResult(images);
                                    }
                                }
                            }).show();
                }
            });
        }

        left_back.setOnClickListener(this);
        initViewPageAdapterData();
    }

    private void initViewPageAdapterData() {
        tv_title.setText(position + 1 + "/" + images.size());
        adapter = new SimpleFragmentAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                PictureExternalPreviewActivity.this.position = position;
                tv_title.setText((position + 1) + "/" + images.size());
//                tv_title.setText(position + 1 + "/" + images.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        finish();
    }

    public class SimpleFragmentAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            (container).removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View contentView = inflater.inflate(R.layout.picture_image_preview, container, false);
            // 常规图控件
            final PhotoView imageView = (PhotoView) contentView.findViewById(R.id.preview_image);
            // 长图控件
            final SubsamplingScaleImageView longImg = (SubsamplingScaleImageView) contentView.findViewById(R.id.longImg);

            LocalMedia media = images.get(position);
            if (media != null) {
                String pictureType = media.getPictureType();
                final String path;
                if (media.isCut() && !media.isCompressed()) {
                    // 裁剪过
                    path = media.getCutPath();
                } else if (media.isCompressed() || (media.isCut() && media.isCompressed())) {
                    // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                    path = media.getCompressPath();
                } else {
                    path = media.getPath();
                }
                boolean isHttp = PictureMimeType.isHttp(path);
//                if (isHttp) {
//                    pictureType = PictureFileUtils.getMimeType(media.getPath());
//                }
                // 可以长按保存并且是网络图片显示一个对话框
                if (isHttp&&shouldShowDialog) {
                    showPleaseDialog();
                }
                boolean isGif = PictureMimeType.isGif(pictureType);
//                boolean isGif = PictureMimeType.isImageGif(path);
                final boolean eqLongImg = PictureMimeType.isLongImg(media);
                imageView.setVisibility(eqLongImg && !isGif ? View.GONE : View.VISIBLE);
                longImg.setVisibility(eqLongImg && !isGif ? View.VISIBLE : View.GONE);
                // 压缩过的gif就不是gif了
                if (isGif && !media.isCompressed()) {
                    RequestOptions gifOptions = new RequestOptions()
                            .override(480, 800)
                            .priority(Priority.HIGH)
                            .diskCacheStrategy(DiskCacheStrategy.NONE);
                    Glide.with(PictureExternalPreviewActivity.this)
                            .asGif()
                            .apply(gifOptions)
                            .load(path)
                            .listener(new RequestListener<GifDrawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model
                                        , Target<GifDrawable> target, boolean isFirstResource) {
                                    dismissDialog();
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(GifDrawable resource, Object model
                                        , Target<GifDrawable> target, DataSource dataSource,
                                                               boolean isFirstResource) {
                                    dismissDialog();
                                    return false;
                                }
                            })
                            .into(imageView);
                } else {
                    RequestOptions options = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL);
                    Glide.with(PictureExternalPreviewActivity.this)
                            .asBitmap()
                            .load(path)
                            .apply(options)
                            .into(new SimpleTarget<Bitmap>(480, 800) {
                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    dismissDialog();
                                }

                                @Override
                                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                    dismissDialog();
                                    if (eqLongImg) {
                                        displayLongPic(resource, longImg);
                                    } else {
                                        imageView.setImageBitmap(resource);
                                    }
                                }
                            });
                }
                imageView.setOnViewTapListener(new OnViewTapListener() {
                    @Override
                    public void onViewTap(View view, float x, float y) {
                        finish();
                    }
                });
                longImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }
            (container).addView(contentView, 0);
            return contentView;
        }
    }

    /**
     * 加载长图
     *
     * @param bmp
     * @param longImg
     */
    private void displayLongPic(Bitmap bmp, SubsamplingScaleImageView longImg) {
        longImg.setQuickScaleEnabled(true);
        longImg.setZoomEnabled(true);
        longImg.setPanEnabled(true);
        longImg.setDoubleTapZoomDuration(100);
        longImg.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);
        longImg.setDoubleTapZoomDpi(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER);
        longImg.setImage(ImageSource.cachedBitmap(bmp), new ImageViewState(0, new PointF(0, 0), 0));
    }
    private String imgSuffix = ".png";
    protected void savePic(final String path){
        boolean isGif = false;
        for (LocalMedia image : images) {
            if (image.getPath().equals(path)) {
                isGif = PictureMimeType.isGif(image.getPictureType());
                break;
            }
        }
        if (isGif) {
            imgSuffix = ".gif";
        }else{
            imgSuffix = ".png";
        }
        if (rxPermissions == null) {
            rxPermissions = new RxPermissions(PictureExternalPreviewActivity.this);
        }
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            showPleaseDialog();
                            boolean isHttp = PictureMimeType.isHttp(path);
                            if (isHttp) {
                                loadDataThread = new loadDataThread(path);
                                loadDataThread.start();
                            } else {
                                // 有可能本地图片
                                try {
                                    String dirPath = PictureFileUtils.createDir(PictureExternalPreviewActivity.this,
                                            System.currentTimeMillis() + imgSuffix, directory_path);
                                    PictureFileUtils.copyFile(path, dirPath);
                                    noticeTuku(dirPath);
                                    ToastManage.s(mContext, getString(R.string.picture_save_success) + "\n" + dirPath);
                                    dismissDialog();
                                } catch (IOException e) {
                                    ToastManage.s(mContext, getString(R.string.picture_save_error) + "\n" + e.getMessage());
                                    dismissDialog();
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            ToastManage.s(mContext, getString(R.string.picture_jurisdiction));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }


    // 进度条线程
    public class loadDataThread extends Thread {
        private String path;

        public loadDataThread(String path) {
            super();
            this.path = path;
        }

        @Override
        public void run() {
            try {
                showLoadingImage(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 下载图片保存至手机
    public void showLoadingImage(String urlPath) {
        try {
            URL u = new URL(urlPath);
            String path = PictureFileUtils.createDir(PictureExternalPreviewActivity.this,
                    System.currentTimeMillis() + imgSuffix, directory_path);
            byte[] buffer = new byte[1024 * 8];
            int read;
            int ava = 0;
            long start = System.currentTimeMillis();
            BufferedInputStream bin;
            bin = new BufferedInputStream(u.openStream());
            BufferedOutputStream bout = new BufferedOutputStream(
                    new FileOutputStream(path));
            while ((read = bin.read(buffer)) > -1) {
                bout.write(buffer, 0, read);
                ava += read;
                long speed = ava / (System.currentTimeMillis() - start);
            }
            bout.flush();
            bout.close();
            Message message = handler.obtainMessage();
            message.what = 200;
            message.obj = path;
            handler.sendMessage(message);
        } catch (IOException e) {
            ToastManage.s(mContext, getString(R.string.picture_save_error) + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private MediaScannerConnection mMediaScanner;
    private void noticeTuku(String path){
        final File imgFile = new File(path);
        mMediaScanner = new MediaScannerConnection(this, new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                mMediaScanner.scanFile(imgFile.getPath(), "image/jpeg");
                mMediaScanner.scanFile(imgFile.getPath(), "image/png");
                mMediaScanner.scanFile(imgFile.getPath(), "image/gif");
            }

            @Override
            public void onScanCompleted(String path, Uri uri) {

            }
        });
        mMediaScanner.connect();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 200:
                    String path = (String) msg.obj;
                    noticeTuku(path);
                    ToastManage.s(mContext, getString(R.string.picture_save_success) + "\n" + path);
                    dismissDialog();
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (type==2) {
            onResult(images);
        }else{
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadDataThread != null) {
            handler.removeCallbacks(loadDataThread);
            loadDataThread = null;
        }
    }
}
