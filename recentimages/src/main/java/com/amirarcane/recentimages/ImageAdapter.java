package com.amirarcane.recentimages;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jess.ui.TwoWayAbsListView;

public class ImageAdapter extends CursorAdapter {

    public static final int IMAGE_ID_COLUMN = 0;
    public static final int IMAGE_DATA_COLUMN = 1;
    public static final int IMAGE_NAME_COLUMN = 2;
    public static final int IMAGE_DATE_COLUMN = 3;
    public static final int IMAGE_TYPE_COLUMN = 4;

    public static float IMAGE_WIDTH = 100;
    public static float IMAGE_HEIGHT = 100;
    public static float IMAGE_PADDING = 0;

    private final Context mContext;
    private float mScale;
    private int mImageWidth;
    private int mImageHeight;
    private int mImagePadding;

    public ImageView.ScaleType SCALE_TYPE = ImageView.ScaleType.CENTER_CROP;
    public static int KIND = MediaStore.Images.Thumbnails.MINI_KIND;

    public static int DRAWABLE = R.drawable.spinner_black_76;

    public ImageAdapter(Context context, Cursor c) {
        this(context, c, true);
    }

    public ImageAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mContext = context;
        mScale = mContext.getResources().getDisplayMetrics().density;
        mImageWidth = (int) (IMAGE_WIDTH * mScale);
        mImageHeight = (int) (IMAGE_HEIGHT * mScale);
        mImagePadding = (int) (IMAGE_PADDING * mScale);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String path = cursor.getString(IMAGE_DATA_COLUMN);
        RequestOptions requestOptions = new RequestOptions().placeholder(DRAWABLE);
        Glide.with(context)
                .load(Uri.parse("file://" + path))
                .apply(requestOptions)
                .into((ImageView) view);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ImageView imageView = new ImageView(mContext.getApplicationContext());
        imageView.setLayoutParams(new TwoWayAbsListView.LayoutParams(mImageWidth, mImageHeight));
        imageView.setPadding(mImagePadding, mImagePadding, mImagePadding, mImagePadding);
        imageView.setScaleType(SCALE_TYPE);
        return imageView;
    }
}

