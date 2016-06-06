package com.amirarcane.sample;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.amirarcane.recentimages.RecentImages;
import com.amirarcane.recentimages.thumbnailOptions.ImageAdapter;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	private Uri imageUri;
	ArrayList<MenuItem> menuItems = new ArrayList<>();

	private ImageView image;
	private static final int TAKE_PICTURE = 0;

	private static final int SELECT_PHOTO = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final View bottomSheet = getLayoutInflater().inflate(R.layout.bottom_sheet, null);
		image = (ImageView) findViewById(R.id.imageView);
		final TwoWayGridView gridview = (TwoWayGridView) bottomSheet.findViewById(R.id.gridview);

		final Dialog mBottomSheetDialog = new Dialog(this, R.style.MaterialDialogSheet);
		mBottomSheetDialog.setContentView(bottomSheet);
		mBottomSheetDialog.setCancelable(true);
		mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);

		menuItems.add(new MenuItem("Camera", R.drawable.ic_local_see_black_48dp));
		menuItems.add(new MenuItem("Gallery", R.drawable.ic_action_image));

		RecyclerView menu = (RecyclerView) bottomSheet.findViewById(R.id.menu);
		MenuAdapter menuAdapter = new MenuAdapter(menuItems);
		menu.setLayoutManager(new LinearLayoutManager(this));
		menu.setAdapter(menuAdapter);

		menu.addOnItemTouchListener(new RecyclerItemClickListener(this, menu, new RecyclerItemClickListener.OnItemClickListener() {
			@Override
			public void onItemClick(View view, int i) {
				switch (i) {
					case 0:
						takePhoto(view);
						mBottomSheetDialog.dismiss();
						break;
					case 1:
						Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
						photoPickerIntent.setType("image/*");
						startActivityForResult(photoPickerIntent, SELECT_PHOTO);
						mBottomSheetDialog.dismiss();
						break;
				}
			}

			@Override
			public void onItemLongClick(View view, int position) {

			}
		}));
		Button button = (Button) findViewById(R.id.button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mBottomSheetDialog.show();

				RecentImages ri = new RecentImages();
				ImageAdapter adapter = ri.getAdapter(MainActivity.this);

				gridview.setAdapter(adapter);
				gridview.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {
					public void onItemClick(TwoWayAdapterView parent, View v, int position, long id) {
						imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
						Bitmap bitmap = null;
						try {
							bitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), imageUri);
						} catch (IOException e) {
							Log.e(TAG, "Exception while getting image", e);
						}
						image.setImageBitmap(bitmap);
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
		Bitmap bitmap = null;
		try {
			bitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), imageUri);
		} catch (IOException e) {
			Log.e(TAG, "Exception while getting image", e);
		}
		image.setImageBitmap(bitmap);
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
}
