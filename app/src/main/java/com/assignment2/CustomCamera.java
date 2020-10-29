package com.assignment2;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Camera Activity is used to launch the custom Camera Interface
 */
public class CustomCamera extends AppCompatActivity {

    private Camera mCamera; // instance of the device camera
    private CameraView mPreview; //Instance of the camera preview frame
    private ImageView cameraBTN; // Button to capture photo in camera view
    Context context = this; //Reference to the current context.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mCamera = getCameraInstance();
        cameraBTN = findViewById(R.id.cameraBTN);
        mPreview = new CameraView(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraView);
        preview.addView(mPreview);

        cameraBTN = findViewById(R.id.cameraBTN);
        // Create our Preview view and set it as the content of our activity.
        cameraBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null,null,mPicture);
            }
        });
    }

    /**
     *
     * @return Camera instance, ready to be used by class.
     */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /**
     * Picture callback inner anonymous method is used to retrieve picture data after the user
     * has taken a photo.
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            // Get the image taken by the user.
            Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
            // Init Intent
            Intent intent = new Intent(context,Preview.class);
            //Store image in a "Extra"
            Matrix matrix = new Matrix(); // Get matrix to rotate image

            matrix.postRotate(90); // Rotate the image by 90 degrees

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true); // scale the bitmap, and set it to rotate

            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true); // Create new rotated bitmap

            intent.putExtra("uri",insertImage(getContentResolver(),rotatedBitmap,"Image"+System.currentTimeMillis(),"This is a image taken using the camera")); // Put the bitmap in the intent
            //Start Intent
            startActivity(intent);
            //Make camera usable for next session.
            camera.startPreview();


        }



    };


    /**
     *
     * @param cr Current Content Resolver of the device
     * @param source Source Bitmap Image.
     * @param title Title of the bitmap
     * @param description Description of the Bitmap
     * @return Uri pointing to the file.
     */
    public static final Uri insertImage(ContentResolver cr,
                                        Bitmap source,
                                        String title,
                                        String description) {

        ContentValues values = new ContentValues();
        // Put variables to persist in device
        values.put(Images.Media.TITLE, title);
        values.put(Images.Media.DISPLAY_NAME, title);
        values.put(Images.Media.DESCRIPTION, description);
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        // Add the date meta data to ensure the image is added at the front of the gallery
        values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());

        //set URI to NUll
        Uri url = null;
        String stringUrl = null;    /* value to be returned */

        try {
            // try to insert image
            url = cr.insert(Images.Media.EXTERNAL_CONTENT_URI, values);

            if (source != null) {
                OutputStream imageOut = cr.openOutputStream(url);
                try {
                    //Compress images to 50 per cent so they are smaller and much easier to upload.
                    source.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
                } finally {
                    imageOut.close();
                }

            } else {
                cr.delete(url, null, null);
                url = null;
            }
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
                url = null;
            }
        }

        if (url != null) {
            stringUrl = url.toString();
        }

        return url;
    }
}