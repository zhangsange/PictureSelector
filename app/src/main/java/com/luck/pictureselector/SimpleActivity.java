package com.luck.pictureselector;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.ArrayList;
import java.util.List;

public class SimpleActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_activity, btn_fragment;
    private List<LocalMedia> selectedImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);
        btn_activity = (Button) findViewById(R.id.btn_activity);
        btn_fragment = (Button) findViewById(R.id.btn_fragment);
        btn_activity.setOnClickListener(this);
        btn_fragment.setOnClickListener(this);
        findViewById(R.id.btn_preview).setOnClickListener(this);
        selectedImages = new ArrayList<>();
        LocalMedia media = new LocalMedia();
        media.setPath("https://p3.pstatp.com/large/pgc-image/44badde0d814456385d929b13a8e3894");
        selectedImages.add(media);

        LocalMedia media1 = new LocalMedia();
        media1.setPath("https://p3.pstatp.com/large/pgc-image/f3849dc1f4dd45d8bdf39ca54237f39e");
        selectedImages.add(media1);

        LocalMedia media2 = new LocalMedia();
        media2.setPath("https://p1.pstatp.com/large/pgc-image/0ed48b04605a4124b4d252d98f207ab1");
        selectedImages.add(media2);

        LocalMedia media3 = new LocalMedia();
        media3.setPath("https://p1.pstatp.com/large/pgc-image/7a8dd666484f409c98d4829b870a03e1");
        selectedImages.add(media3);

        LocalMedia media4 = new LocalMedia();
        media4.setPath("https://p3.pstatp.com/large/pgc-image/f3849dc1f4dd45d8bdf39ca54237f39e");
        selectedImages.add(media4);

    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btn_activity:
                intent = new Intent(SimpleActivity.this, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_fragment:
                intent = new Intent(SimpleActivity.this, PhotoFragmentActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_preview:
                PictureSelector.create(this).themeStyle(R.style.picture_default_style).openExternalPreview(0,  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(), selectedImages,false);
                break;
        }
    }
}
