package com.amirarcane.sample;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int TAKE_PICTURE = 0;
    private static final int SELECT_PHOTO = 1;

    ArrayList<MenuItem> menuItems = new ArrayList<>();

    private Uri imageUri;
    private ImageView imageView;
    private ContentResolver contentResolver;
    private File photoFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Util.checkPermission(
                this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                },
                new Util.OnPermissionCallback() {
                    @Override
                    public void onPermissionGranted() {
                    }

                    @Override
                    public void onPermissionDenied() {
                        finish();
                    }
                }
        );

        final View bottomSheet = getLayoutInflater().inflate(R.layout.bottom_sheet, null);
        imageView = (ImageView) findViewById(R.id.imageView);
        final TwoWayGridView twoWayGridView = (TwoWayGridView) bottomSheet.findViewById(R.id.gridview);
        Button button = (Button) findViewById(R.id.choose);
        Button clearCache = (Button) findViewById(R.id.clearCache);

        final Dialog mBottomSheetDialog = new Dialog(this, R.style.MaterialDialogSheet);
        mBottomSheetDialog.setContentView(bottomSheet);
        mBottomSheetDialog.setCancelable(true);
        mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);

        menuItems.add(new MenuItem("Camera", R.drawable.ic_local_see_black_48dp));
        menuItems.add(new MenuItem("Gallery", R.drawable.ic_action_image));

        contentResolver = this.getContentResolver();

        RecyclerView menu = (RecyclerView) bottomSheet.findViewById(R.id.menu);
        MenuAdapter menuAdapter = new MenuAdapter(menuItems);
        menu.setLayoutManager(new LinearLayoutManager(this));
        menu.setAdapter(menuAdapter);

        final RecentImages recentImages = new RecentImages();
        final ImageAdapter adapter = recentImages.getAdapter(MainActivity.this);

        menu.addOnItemTouchListener(new RecyclerItemClickListener(this, menu, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int i) {
                if (i == 0) {
                    takeImage();
                    mBottomSheetDialog.dismiss();
                } else if (i == 1) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("imageView/*");
                    startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                    mBottomSheetDialog.dismiss();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        }));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.show();

                twoWayGridView.setAdapter(adapter);
                twoWayGridView.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {
                    public void onItemClick(TwoWayAdapterView parent, View v, int position, long id) {
                        imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        Bitmap bitmap = null;
                        Drawable d = null;
                        try {
                            int orientation = getOrientation(contentResolver, (int) id);
                            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
                            d = getRotateDrawable(bitmap, orientation);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        imageView.setImageDrawable(d);
                        mBottomSheetDialog.dismiss();
                    }
                });
            }
        });

        clearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recentImages.cleanupCache();
            }
        });
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

    private void takeImage() {
        Intent capturePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        capturePhotoIntent.putExtra("return-data", true);

        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (photoFile != null) {
            Uri photoURI = FileProvider
                    .getUriForFile(MainActivity.this, "com.amirarcane.sample", photoFile);
            capturePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(capturePhotoIntent, TAKE_PICTURE);
        }
    }

    private File createImageFile() throws IOException {
        // Create an imageView file name
        final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timeStamp = DATE_FORMAT.format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File cacheDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                cacheDir      /* directory */
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_PHOTO:
                    imageUri = data.getData();
                    if (imageUri != null) {
                        Bitmap bitmap = null;

                        try {
                            Log.d("ImageURI", String.valueOf(imageUri));
                            bitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), imageUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        imageView.setImageBitmap(bitmap);
                    }
                    break;
                case TAKE_PICTURE:
                    Uri imageUri = null;

                    if (data == null || data.getData() == null
                            || data.getData().equals(Uri.fromFile(photoFile))) {
                        imageUri = Uri.fromFile(photoFile);
                    } else if (null != data.getData()) {
                        imageUri = data.getData();
                    }
                    if (null != imageUri) {
                        imageView.setImageURI(imageUri);
                    }
                    break;
            }
        }
    }

    private int getOrientation(ContentResolver cr, int id) {

        String photoID = String.valueOf(id);

        Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media.ORIENTATION}, MediaStore.Images.Media._ID + "=?",
                new String[]{"" + photoID}, null);
        int orientation = -1;

        if (cursor.getCount() != 1) {
            return -1;
        }

        if (cursor.moveToFirst()) {
            orientation = cursor.getInt(0);
        }
        cursor.close();
        return orientation;
    }

}
