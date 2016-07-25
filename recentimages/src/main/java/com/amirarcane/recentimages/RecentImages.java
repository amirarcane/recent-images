package com.amirarcane.recentimages;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.amirarcane.recentimages.thumbnailOptions.ImageAdapter;

/**
 * Created by Arcane on 10/30/15 AD.
 */
public class RecentImages {

	public static final String ASCENDING = " ASC";
	public static final String DESCENDING = " DESC";
	public static final String DESCRIPTION = "description";
	public static final String PICASA_ID = "picasa_id";
	public static final String IS_PRIVATE = "isprivate";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String DATE_TAKEN = "datetaken";
	public static final String ORIENTATION = "orientation";
	public static final String MINI_THUMB_MAGIC = "mini_thumb_magic";
	public static final String BUCKET_ID = "bucket_id";
	public static final String BUCKET_DISPLAY_NAME = "bucket_display_name";

	public ImageAdapter getAdapter(Context context) {
		return getAdapter(context, DATE_TAKEN, DESCENDING);
	}

	public ImageAdapter getAdapter(Context context, String columns, String sort) {
		Cursor mImageCursor = null;
		try {
			String[] projection = new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA,
					MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.MIME_TYPE};
			mImageCursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, columns + sort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ImageAdapter mAdapter = new ImageAdapter(context, mImageCursor);
		return mAdapter;
	}

	public void setDrawable(int drawable) {
		ImageAdapter.DRAWABLE = drawable;
	}

	public void setHeight(int height) {
		ImageAdapter.IMAGE_HEIGHT = height;
	}

	public void setWidth(int width) {
		ImageAdapter.IMAGE_WIDTH = width;
	}

	public void setPadding(int padding) {
		ImageAdapter.IMAGE_PADDING = padding;
	}

	public void setSize(int size) {
		ImageAdapter.IN_SAMPLE_SIZE = size;
	}

	public void setKind(int kind) {
		ImageAdapter.KIND = kind;
	}

}
