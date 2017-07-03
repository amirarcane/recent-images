package com.amirarcane.recentimagesapp;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amirarcane.recentimages.ImageAdapter;
import com.amirarcane.recentimages.RecentImages;
import com.bumptech.glide.Glide;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	private Uri imageUri;
	private ImageView mImage;
	private ContentResolver cr;
    private Context mContext;

	private static final int TAKE_PICTURE = 0;
	private static final int SELECT_PHOTO = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mContext = getApplicationContext();

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

		cr = this.getContentResolver();

        LinearLayout layoutCamera = (LinearLayout) bottomSheet.findViewById(R.id.btn_camera);
        LinearLayout layoutGallery = (LinearLayout) bottomSheet.findViewById(R.id.btn_gallery);
        layoutCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto(view);
                mBottomSheetDialog.dismiss();
            }
        });
        layoutGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("mImage/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                mBottomSheetDialog.dismiss();
            }
        });

        mImage = (ImageView) findViewById(R.id.imageView);
        final TwoWayGridView gridview = (TwoWayGridView) bottomSheet.findViewById(R.id.gridview);

        CustomButton button = (CustomButton) findViewById(R.id.button);
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
                        Glide.with(mContext).load(imageUri).into(mImage);
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
            Glide.with(mContext).load(imageUri).into(mImage);
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
}
