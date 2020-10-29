package com.assignment2;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;



public class Main extends AppCompatActivity {

    private GridView grid;    // Gridview to be displayed on main homescreen
    private Button sync;
    private List<Uri> uris;
    private Context context = this;
    private boolean mobileConnected = false;
    private FirebaseStorage mStorageReferece;
    private StorageReference mStorageRef;
    private StorageTask<UploadTask.TaskSnapshot> storeImage;
    private Button download;
    private Button capture;
    private List<String> fileNames;
    private TextView blankHint;
    private Adapter gridAdapter;
    private int batteryLevel;

    /**
     * Initialise the main screen, load all assets
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // Set the layout from the .xml configuration file.
        askPermissions(); // Ask for permissions for accessing internal storage, external storage, camera and network state.
        findAllUris(); // Find URIs of all image based files on users device
        mStorageReferece = FirebaseStorage.getInstance();
        mStorageRef = mStorageReferece.getReference();
        loadScene();
        syncAll();
        startCapture();
        deleteItem();
        checkBatteryLevel();
        syncRequest();
        startAsyncTask();
        if(getBatteryState() > 10 && isConnectedToWifi())
        {
            aSyncUploadTask();
        }


    }

    /**
     * Start camera activity upon user button click
     */
    private void startCapture() {
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CustomCamera.class);  //Init Intent
                startActivity(intent); //Explode Activity
            }
        });
    }

    /**
     *
     * @return A list of URIs, pointing to images in the users
     * device.
     */
    public List<Uri> findAllUris()
    {

        uris = new ArrayList<>();  //init list of uris
        fileNames = new ArrayList<>();
        Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI; //get File names of all photos in users device

        String[] projection = { MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME};   // Specify what the cursor, should look for, in particular we want the ID and Name

        Cursor cursor = this.managedQuery(uri, projection, null, null, null); //tell cursor to look for uri, id and name
        int  column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID); //get index of ID
        int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME); //get index of Name

        while (cursor.moveToNext()) {  // Search for all images in users device
            long id2 = cursor.getLong(column_index);
            String name = cursor.getString(nameColumn);
            fileNames.add(name);
            Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,id2);
            uris.add(contentUri); // add uri to list of uris

        }
        return uris;

    }

    /**
     *  Sync all items from the users gallery to the cloud storage bucket/instance
     */
    public void syncAll()
    {
        sync = findViewById(R.id.sync);
        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(int i = 0; i<uris.size(); ++i)
                {
                    //access the uri list
                    final int finalI = i;
                    storeImage = mStorageRef.child("images/" +fileNames.get(i)).putFile(uris.get(i)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>()  { // put every uri into the database
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            Toast.makeText(context,"Syncing" + fileNames.get(finalI),Toast.LENGTH_SHORT).show();  // print a toast saying the task has succeeded.
                        }
                    });
                }
            }
        });
    }

    public void getSyncedItems()
    {
        final StorageReference listRef = mStorageReferece.getReference().child("images/"); //get a reference to the bucket/folder containing the images
        final long ONE_MEGABYTE = 1024 * 1024; // declare a final variable this the maximum size that we can persist/extract from the database
        listRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                for (final StorageReference item : listResult.getItems()) {  // Firstly we access all the items in the bucket
                    StorageReference islandRef = mStorageRef.child("images/"+item.getName());
                    islandRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length); // we then retrieve the images as a byte array, which is then converted to a bitmap
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 50, out);  // The images are then further compressed
                            Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));  //  the decoded bitmap
                            CustomCamera.insertImage(getContentResolver(),decoded,item.getName(),"Some Image"); // decoded bitmap is persisted locally on users device.
                            uris = findAllUris(); // Gets all the URIs that have been added to our device
                        }
                    });
                }
            }
        });
        gridAdapter.setBmps(uris); //resets the adapter
        Toast.makeText(this,"Restart app,to view synced library",Toast.LENGTH_LONG).show(); //prompts user to restart
        gridAdapter.notifyDataSetChanged(); // data is all updated and persisted.
    }


    /**
     * Given the URI, we want to return the file name of the image.
     * @param uri of file
     * @return file name of uri
     */
    public String getFileNameFromURI(Uri uri)
    {
        String[] projection = { MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME};  // Tell the cursor what to search for

        String name = null;  // set Name to null
        Cursor cursor = this.managedQuery(uri, projection, null, null, null);  //Load cursor parameters
        int  column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);  //specify indexes for ID and Name
        int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME); // set Display Name index
        while (cursor.moveToNext()) {   // Loop over all existing files
            long id2 = cursor.getLong(column_index);
            name = cursor.getString(nameColumn);
            Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,id2);  // Get the URI of the file name
            if(contentUri.toString().equals(uri.toString()))  // Ensure the URI matches with content
            {
                if (name.indexOf(".") > 0)
                {
                    name = name.substring(0, name.lastIndexOf("."));
                    return name;  //Return the name of the file.
                }
            }
            else
            {
                throw new IllegalStateException("Catastrophic failure");  // If the file does not exist return illegalState exceptions
            }

        }

        return name;
    }

    /**
     *
     * @param nameOfFile File to be deleted from Cloud Storage database.
     */



    /**
     *
     * @return true or False if the mobile is connected to mobile data/network
     */
    private boolean isConnectedToMobile()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected())
        {
            return   mobileConnected = networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        }
        return false;
    }

    /**
     * Run a asynchronous syncing task while the device battery is greater than 10% and connected to Wi-Fi.
     */
    private void aSyncUploadTask()
    {
        final Handler handler = new Handler();  // Instantiate new thread handler
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try{
                    if(uris.size() != 0 && fileNames.size()!= 0) {  // If URi and Files are present
                        for (int i = 0; i < uris.size(); ++i) {
                            storeImage = mStorageRef.child("images/" + fileNames.get(i)).putFile(uris.get(i)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {  // Start sync and load onCompleteListener
                                    Toast.makeText(context, "Syncing in background please wait", Toast.LENGTH_SHORT).show();  // Toast highlighting files are syncing.
                                }
                            });
                        }
                    }
                }
                catch (Exception e) { // Catch exception
                }
                finally{
                    handler.postDelayed(this, 50000); //Sync files every 50 seconds
                }
            }
        };

//runnable must be execute once
        handler.post(runnable);
    }


    /**
     * Return the battery level of the device
     * @return Battery Level of the device.
     */
    public int getBatteryState()
    {
        // Access battery manager.
        BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
        //Retrieve battery level using getIntProperty();
        batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return batteryLevel;
    }

    /**
     * Helper method used to display Toast, if battery is under 10%
     */
    public void checkBatteryLevel()
    {
        if(getBatteryState() < 10)
        {
            Toast.makeText(this,"Disabling background sync: Low Battery!",Toast.LENGTH_LONG).show();
        }
        else Log.d("SUCCESS","starting Async Background tasks");
    }

    /**
     * Method used to delete item from database
     */
    public void deleteItem()
    {
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {  // On item click
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                uris.remove(i); //Remove from list
                gridAdapter.notifyDataSetChanged(); //Notify adapter
                Toast.makeText(context,"Item deleted locally and in the cloud",Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }

    /**
     * Load all assets in the scene.
     */
    public void loadScene()
    {
        download = findViewById(R.id.download);
        blankHint = findViewById(R.id.blankHint);
        grid = findViewById(R.id.grid);
        gridAdapter = new Adapter(this,findAllUris());
        grid.setAdapter(gridAdapter);
        blankHint.setText("Please take some photos!");
        grid.setEmptyView(blankHint);
        capture = findViewById(R.id.capture);
    }


    /**
     * Sync items upon button click.
     */
    public void syncRequest()
    {
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSyncedItems();
            }
        });
    }

    /**
     * Check if async task can be started.
     */
    public void startAsyncTask()
    {

        if(isConnectedToMobile())
        {
            Toast.makeText(this,"Device connected to mobile, disabling background sync",Toast.LENGTH_LONG).show();
        }else if(getBatteryState() < 10 && isConnectedToWifi())
        {
            Toast.makeText(this,"Device battery is too low, please sync manually",Toast.LENGTH_LONG).show();
        }


    }


    /**
     * Check if device is connected to Wifi
     * @return True if connected else return False..
     */
    private boolean isConnectedToWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected())
        {
            return   mobileConnected = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }


    /**
     * Ask for device permissions from the manifest.
     */
    public void askPermissions() {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};  // Ask for permission for Camera, External Storage and and Read/Write.

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED) {


        } else {
            ActivityCompat.requestPermissions(this, permissions, 1001);
            Toast.makeText(this, "PLEASE RESTART AFTER CONFIRMING PERMISSIONS TO VIEW GALLERY", Toast.LENGTH_LONG).show();// Display restart warning after confirmation.
            Toast.makeText(this, "PLEASE RESTART AFTER CONFIRMING PERMISSIONS TO VIEW GALLERY", Toast.LENGTH_LONG).show();//Display restart warning after confirmation
            Toast.makeText(this, "PLEASE RESTART AFTER CONFIRMING PERMISSIONS TO VIEW GALLERY", Toast.LENGTH_LONG).show();//Display restart warning after confirmation
            Toast.makeText(this, "PLEASE RESTART AFTER CONFIRMING PERMISSIONS TO VIEW GALLERY", Toast.LENGTH_LONG).show();//Display restart warning after confirmation


        }
    }
}