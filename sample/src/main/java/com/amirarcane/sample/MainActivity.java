package com.amirarcane.sample;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amirarcane.recentimages.RecentImages;
import com.amirarcane.recentimages.ImageAdapter;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	private Uri imageUri;
	ArrayList<MenuItem> menuItems = new ArrayList<>();
	private TwoWayGridView mImageGrid;
	private ImageView image;
	private ContentResolver cr;

	private static final int TAKE_PICTURE = 0;
	private static final int SELECT_PHOTO = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Permissions need to be granted at runtime on Marshmallow
		if (Build.VERSION.SDK_INT >= 21) {
			CheckPermissions();
		}

        final View bottomSheet = getLayoutInflater().inflate(R.layout.bottom_sheet, null);

		final Dialog mBottomSheetDialog = new Dialog(this, R.style.MaterialDialogSheet);
		mBottomSheetDialog.setContentView(bottomSheet);
		mBottomSheetDialog.setCancelable(true);
		mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);

		menuItems.add(new MenuItem("Camera", R.drawable.ic_local_see_black_48dp));
		menuItems.add(new MenuItem("Gallery", R.drawable.ic_action_image));

		cr = this.getContentResolver();

		RecyclerView menu = (RecyclerView) bottomSheet.findViewById(R.id.menu);
		MenuAdapter menuAdapter = new MenuAdapter(menuItems);
		menu.setLayoutManager(new LinearLayoutManager(this));
		menu.setAdapter(menuAdapter);

		menu.addOnItemTouchListener(new RecyclerItemClickListener(this, menu, new RecyclerItemClickListener.OnItemClickListener() {
			@Override
			public void onItemClick(View view, int i) {
				if (i == 0) {
					takePhoto(view);
					mBottomSheetDialog.dismiss();
				} else if (i == 1) {
					Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
					photoPickerIntent.setType("image/*");
					startActivityForResult(photoPickerIntent, SELECT_PHOTO);
					mBottomSheetDialog.dismiss();
				}
			}

			@Override
			public void onItemLongClick(View view, int position) {

			}
		}));

        image = (ImageView) findViewById(R.id.imageView);
        final TwoWayGridView gridview = (TwoWayGridView) bottomSheet.findViewById(R.id.gridview);

		Button button = (Button) findViewById(R.id.button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mBottomSheetDialog.show();

				final RecentImages ri = new RecentImages();
				ImageAdapter adapter = ri.getAdapter(MainActivity.this);

				gridview.setAdapter(adapter);
				gridview.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {
					public void onItemClick(TwoWayAdapterView parent, View v, int position, long id) {
						imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
						Bitmap bitmap = null;
						Drawable d = null;
						try {
							int orientation = getOrientation(cr, (int) id);
							bitmap = MediaStore.Images.Media.getBitmap(cr, imageUri);
							d = getRotateDrawable(bitmap, orientation);
						} catch (IOException e) {
							e.printStackTrace();
						}
						image.setImageDrawable(d);
						mBottomSheetDialog.dismiss();
					}
				});
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case TAKE_PICTURE:
				if (resultCode == Activity.RESULT_OK) {
				}
				break;
			case SELECT_PHOTO:
				if (resultCode == Activity.RESULT_OK) {
						imageUri = data.getData();
				}
				break;
		}
		if (imageUri != null) {
			Bitmap bitmap = null;
			try {
				Log.d("ImageURI", String.valueOf(imageUri));
				bitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), imageUri);
			} catch (IOException e) {
				e.printStackTrace();
			}
			image.setImageBitmap(bitmap);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case 1:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Toast.makeText(this, "Thanks!",
							Toast.LENGTH_SHORT).show();
				} else {
					//If user denies Storage Permission, explain why permission is needed and prompt again.
					Toast.makeText(this, "Storage access is needed to display images.",
							Toast.LENGTH_SHORT).show();
					CheckPermissions();
				}
				break;
			default:
				break;
		}
	}

	//take photo via camera intent
	public void takePhoto(View view) {
		Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
		String name = String.valueOf(System.currentTimeMillis() + ".jpg");
		File photo = new File(String.valueOf(Environment.getExternalStorageDirectory()), name);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
		imageUri = Uri.fromFile(photo);
		startActivityForResult(intent, TAKE_PICTURE);
	}

	public void CheckPermissions()
	{
		int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		if (permissionCheck != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
		}
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

	private Drawable getRotateDrawable(final Bitmap b, final float angle) {
		final BitmapDrawable drawable = new BitmapDrawable(getResources(), b) {
			@Override
			public void draw(final Canvas canvas) {
				canvas.save();
				canvas.rotate(angle, b.getWidth() / 2, b.getHeight() / 2);
				super.draw(canvas);
				canvas.restore();
			}
		};
		return drawable;
	}
}
