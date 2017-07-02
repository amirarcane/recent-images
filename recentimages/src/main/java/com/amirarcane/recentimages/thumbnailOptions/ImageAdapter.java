package com.amirarcane.recentimages.thumbnailOptions;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.amirarcane.recentimages.R;
import com.jess.ui.TwoWayAbsListView;

import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageAdapter extends CursorAdapter {
	private static final String[] IMAGE_PROJECTION = {
			MediaStore.Images.ImageColumns._ID,
			MediaStore.Images.ImageColumns.DISPLAY_NAME,
	};

	public static final int IMAGE_ID_COLUMN = 0;
	public static final int IMAGE_NAME_COLUMN = 1;

	public static final boolean DEBUG = false;

	private static final String TAG = "ImageAdapter";

	public static float IMAGE_WIDTH = 70;
	public static float IMAGE_HEIGHT = 70;
	public static float IMAGE_PADDING = 0;

	private static final Map<Long, SoftReference<ReplaceableBitmapDrawable>> sImageCache =
			new ConcurrentHashMap<Long, SoftReference<ReplaceableBitmapDrawable>>();

	private static Options sBitmapOptions = new Options();

	private final Context mContext;
	private Bitmap mDefaultBitmap;
	private final ContentResolver mContentResolver;
	private final Handler mHandler;
	private float mScale;
	private int mImageWidth;
	private int mImageHeight;
	private int mImagePadding;

	public ImageView.ScaleType SCALE_TYPE = ImageView.ScaleType.CENTER_CROP;
	public static int IN_SAMPLE_SIZE = 2;
	public static int KIND = MediaStore.Images.Thumbnails.MINI_KIND;

	public static int DRAWABLE = R.drawable.spinner_black_76;

	public ImageAdapter(Context context, Cursor c) {
		this(context, c, true);
	}

	public ImageAdapter(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
		mContext = context;
		init(c);
		mContentResolver = context.getContentResolver();
		mHandler = new Handler();
	}

	private void init(Cursor c) {
		mDefaultBitmap = BitmapFactory.decodeResource(mContext.getResources(), DRAWABLE);

		mScale = mContext.getResources().getDisplayMetrics().density;
		mImageWidth = (int) (IMAGE_WIDTH * mScale);
		mImageHeight = (int) (IMAGE_HEIGHT * mScale);
		mImagePadding = (int) (IMAGE_PADDING * mScale);
		sBitmapOptions.inSampleSize = IN_SAMPLE_SIZE;
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
		int id = cursor.getInt(IMAGE_ID_COLUMN);
		((ImageView) view).setImageDrawable(getCachedThumbnailAsync(
				ContentUris.withAppendedId(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, id)));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ImageView imageView = new BetterImageView(mContext.getApplicationContext());
		imageView.setLayoutParams(new TwoWayAbsListView.LayoutParams(mImageWidth, mImageHeight));
		imageView.setPadding(mImagePadding, mImagePadding, mImagePadding, mImagePadding);
		imageView.setScaleType(SCALE_TYPE);
		return imageView;
	}

	public void cleanup() {
		cleanupCache();
	}


	private static Bitmap loadThumbnail(ContentResolver cr, Uri uri) {

		int id = (int) ContentUris.parseId(uri);
		int orientation = getOrientation(cr, id);
		Matrix matrix = new Matrix();
		matrix.postRotate(orientation);
		Log.d("Orientation", String.valueOf(orientation));
		Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
				cr, id, KIND, sBitmapOptions);
		bitmap = crop(bitmap, matrix);
		return bitmap;
	}

	private static int getOrientation(ContentResolver cr, int id) {

		String photoID = String.valueOf(id);

		Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				new String[] {MediaStore.Images.Media.ORIENTATION}, MediaStore.Images.Media._ID + "=?",
				new String[] {"" + photoID}, null);
		int orientation = -1;

		if (cursor.getCount() != 1) {
			return -1;
		}

		if (cursor.moveToFirst())
		{
			orientation = cursor.getInt(0);
		}
		cursor.close();
		return orientation;
	}

	private static Bitmap crop(Bitmap srcBmp, Matrix matrix) {
		Bitmap dstBmp;
		if (srcBmp.getWidth() >= srcBmp.getHeight()) {

			dstBmp = Bitmap.createBitmap(
					srcBmp,
					srcBmp.getWidth() / 2 - srcBmp.getHeight() / 2,
					0,
					srcBmp.getHeight(),
					srcBmp.getHeight(),
					matrix,
					true
			);

		} else {

			dstBmp = Bitmap.createBitmap(
					srcBmp,
					0,
					srcBmp.getHeight() / 2 - srcBmp.getWidth() / 2,
					srcBmp.getWidth(),
					srcBmp.getWidth(),
					matrix,
					true
			);
		}
		return dstBmp;
	}

	/**
	 * Retrieves a drawable from the image cache, identified by the specified id.
	 * If the drawable does not exist in the cache, it is loaded asynchronously and added to the cache.
	 * If the drawable cannot be added to the cache, the specified default drawable is
	 * returned.
	 *
	 * @param uri The uri of the drawable to retrieve
	 * @return The drawable identified by id or defaultImage
	 */
	private ReplaceableBitmapDrawable getCachedThumbnailAsync(Uri uri) {
		ReplaceableBitmapDrawable drawable = null;
		long id = ContentUris.parseId(uri);

		WorkQueue wq = WorkQueue.getInstance();
		synchronized (wq.mQueue) {
			SoftReference<ReplaceableBitmapDrawable> reference = sImageCache.get(id);
			if (reference != null) {
				drawable = reference.get();
			}

			if (drawable == null || !drawable.isLoaded()) {
				drawable = new ReplaceableBitmapDrawable(mDefaultBitmap);
				sImageCache.put(id, new SoftReference<ReplaceableBitmapDrawable>(drawable));
				ImageLoadingArgs args = new ImageLoadingArgs(mContentResolver, mHandler, drawable, uri);
				wq.execute(new ImageLoader(args));

			}
		}

		return drawable;
	}

	/**
	 * Removes all the callbacks from the drawables stored in the memory cache. This
	 * method must be called from the onDestroy() method of any activity using the
	 * cached drawables. Failure to do so will result in the entire activity being
	 * leaked.
	 */
	public static void cleanupCache() {
		for (SoftReference<ReplaceableBitmapDrawable> reference : sImageCache.values()) {
			final ReplaceableBitmapDrawable drawable = reference.get();
			if (drawable != null) drawable.setCallback(null);
		}
	}

	/**
	 * Deletes the specified drawable from the cache.
	 *
	 * @param uri The uri of the drawable to delete from the cache
	 */
	public static void deleteCachedCover(Uri uri) {
		sImageCache.remove(ContentUris.parseId(uri));
	}


	/**
	 * Class to asynchronously perform the loading of the bitmap
	 */
	public static class ImageLoader implements Runnable {
		protected ImageLoadingArgs mArgs = null;

		public ImageLoader(ImageLoadingArgs args) {
			mArgs = args;
		}

		public void run() {
			final Bitmap bitmap = loadThumbnail(mArgs.mContentResolver, mArgs.mUri);
			if (DEBUG) Log.i(TAG, "run() bitmap: " + bitmap);
			if (bitmap != null) {
				final ReplaceableBitmapDrawable d = mArgs.mDrawable;
				if (d != null) {
					mArgs.mHandler.post(new Runnable() {
						public void run() {
							if (DEBUG)
								Log.i(TAG, "ImageLoader.run() - setting the bitmap for uri: " + mArgs.mUri);
							d.setBitmap(bitmap);
						}
					});
				} else {
					Log.e(TAG, "ImageLoader.run() - FastBitmapDrawable is null for uri: " + mArgs.mUri);
				}
			} else {
				Log.e(TAG, "ImageLoader.run() - bitmap is null for uri: " + mArgs.mUri);
			}
		}

		public void cancel() {
			sImageCache.remove(mArgs.mUri);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof ImageLoader && mArgs.mUri != null) {
				return mArgs.mUri.equals(((ImageLoader) obj).mArgs);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return mArgs.mUri.hashCode();
		}

	}


	/**
	 * Class to hold all the parts necessary to load an image
	 */
	public static class ImageLoadingArgs {
		ContentResolver mContentResolver;
		Handler mHandler;
		ReplaceableBitmapDrawable mDrawable;
		Uri mUri;

		/**
		 * @param contentResolver - ContentResolver to use
		 * @param drawable        - FastBitmapDrawable whose underlying bitmap should be replaced with new bitmap
		 * @param uri             - Uri of image location
		 */
		public ImageLoadingArgs(ContentResolver contentResolver, Handler handler,
		                        ReplaceableBitmapDrawable drawable, Uri uri) {
			mContentResolver = contentResolver;
			mHandler = handler;
			mDrawable = drawable;
			mUri = uri;
		}
	}


	public static class WorkQueue {
		private static WorkQueue sInstance = null;
		private static final int NUM_OF_THREADS = 1;
		private static final int MAX_QUEUE_SIZE = 21;

		private final int mNumOfThreads;
		private final PoolWorker[] mThreads;
		protected final LinkedList<ImageLoader> mQueue;

		public static synchronized WorkQueue getInstance() {
			if (sInstance == null) {
				sInstance = new WorkQueue(NUM_OF_THREADS);
			}
			return sInstance;
		}

		private WorkQueue(int nThreads) {
			mNumOfThreads = nThreads;
			mQueue = new LinkedList<ImageLoader>();
			mThreads = new PoolWorker[mNumOfThreads];

			for (int i = 0; i < mNumOfThreads; i++) {
				mThreads[i] = new PoolWorker();
				mThreads[i].start();
			}
		}

		public void execute(ImageLoader r) {
			synchronized (mQueue) {
				mQueue.remove(r);
				if (mQueue.size() > MAX_QUEUE_SIZE) {
					mQueue.removeFirst().cancel();
				}
				mQueue.addLast(r);
				mQueue.notify();
			}
		}

		private class PoolWorker extends Thread {
			private boolean mRunning = true;

			@Override
			public void run() {
				Runnable r;

				while (mRunning) {
					synchronized (mQueue) {
						while (mQueue.isEmpty() && mRunning) {
							try {
								mQueue.wait();
							} catch (InterruptedException ignored) {
								Thread.currentThread().interrupt();
							}
						}

						r = mQueue.removeFirst();
					}

					// If we don't catch RuntimeException,
					// the pool could leak threads
					try {
						r.run();
					} catch (RuntimeException e) {
						Log.e(TAG, "RuntimeException", e);

					}
				}
				Log.i(TAG, "PoolWorker finished");
			}

			public void stopWorker() {
				mRunning = false;
			}
		}
	}
}

