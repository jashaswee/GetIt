package me.satyabrat.getit;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import me.satyabrat.getit.listeners.PictureCapturingListener;
import me.satyabrat.getit.services.APictureCapturingService;
import me.satyabrat.getit.services.PictureCapturingServiceImpl;

public class MainActivity extends Activity implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        PictureCapturingListener, ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;
    private ImageView uploadBackPhoto;
    private ImageView uploadFrontPhoto;
    private APictureCapturingService pictureService;
    private static final String TAG = "LocationActivity";
    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    TextView tvLocation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;
    int TAKE_PHOTO_CODE = 0;
    public static int count = 0;
    public final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/GetIt/";

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        //show error dialog if GooglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        setContentView(R.layout.activity_main);
        tvLocation = (TextView) findViewById(R.id.tvLocation);
        checkPermissions();
        uploadBackPhoto = (ImageView) findViewById(R.id.backIV);
        uploadFrontPhoto = (ImageView) findViewById(R.id.frontIV);
        pictureService = PictureCapturingServiceImpl.getInstance(this);

        File newdir = new File(dir);
        newdir.mkdirs();

    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * We've finished taking pictures from all phone's cameras
     */
    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            showToast("Done capturing all photos!");
            return;
        }
        showToast("No camera detected!");
    }

    /**
     * Displaying the pictures taken.
     */
    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                if (pictureUrl.contains("0_pic.jpg")) {
                    uploadBackPhoto.setImageBitmap(scaled);
                } else if (pictureUrl.contains("1_pic.jpg")) {
                    uploadFrontPhoto.setImageBitmap(scaled);
                }
            });
            showToast("Picture saved to " + pictureUrl);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }

    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final String[] requiredPermissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }

    public void snapActivity() {

        // Here, the counter will be incremented each time, and the
        // picture taken by camera will be stored as 1.jpg,2.jpg
        // and likewise.
        count++;
        String file = dir + count + ".jpg";
        File newfile = new File(file);
        try {
            newfile.createNewFile();
        } catch (IOException e) {
        }

        Uri outputFileUri = Uri.fromFile(newfile);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

        startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
    }

    private void sendMail() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String loc = preferences.getString("Location", "");
        if (!loc.equalsIgnoreCase("")) {
            //if (!PictureCapturingServiceImpl.file.toString().equalsIgnoreCase(null)) {
                //Log.e(TAG, "file://" + PictureCapturingServiceImpl.file.toString());
                Log.e(TAG, "thisruns");
                BackgroundMail.newBuilder(this)
                        .withUsername("getit.app.antitheft@gmail.com")
                        .withPassword("thuglife69")
                        .withMailto("satya.bshs2014@gmail.com")
                        .withType(BackgroundMail.TYPE_PLAIN)
                        .withSubject("GetIT Anti-Theft Alert")
                        .withBody(loc)
                        //.withAttachments("file://" + PictureCapturingServiceImpl.file.toString())
                        //.withAttachments("file:///storage/emulated/0/0_pic.jpg")
                        .send();

            /*Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("application/image");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, "satya.bshs2014@gmail.com");
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Anti-Theft Alert");
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, coord);
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+PictureCapturingServiceImpl.file.toString()));
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));*/
            //}
        }
    }

    public int flag;

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart fired ");
        mGoogleApiClient.connect();
        //updateUI();
        showToast("Starting capture!");
        pictureService.startCapturing(this);
        flag = 0;
        //sendMail();

        /*Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("application/image");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, "satyabrat.me@gmail.com");
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Anti-Theft Alert");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, coord);*/

        //emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(PictureCapturingServiceImpl.file.toString()));
        //startActivity(Intent.createChooser(emailIntent, "Send mail..."));

        //snapActivity();
        //startService(new Intent(MainActivity.this, CameraService.class));
        Toast.makeText(this, "LongPress Power Button", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop fired ");
        mGoogleApiClient.disconnect();
        Log.i(TAG, "isConnected: " + mGoogleApiClient.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected - isConnected: " + mGoogleApiClient.isConnected());
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
        Log.i(TAG, "Location update started: ");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (flag==0) {
            Log.i(TAG, "Firing onLocationChanged");
            mCurrentLocation = location;
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI();
            //sendMail();
        }
    }

    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public String coord;

    private void updateUI() {
        Log.i(TAG, "UI update initiated");
        if (null != mCurrentLocation) {
            Double lat = mCurrentLocation.getLatitude();
            Double lng = mCurrentLocation.getLongitude();
            coord = "Lat: " + round(lat, 2) + "\n" + "Long: " + round(lng, 2) + "\n";
            tvLocation.setText(coord);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("Location", coord);
            editor.apply();
            flag=1;
        } else {
            Log.i(TAG, "location is null");
        }
    }

    /*@Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }*/

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        Log.i(TAG, "Location update stopped");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
            Log.i(TAG, "Location update resumed");
        }
    }

    /*@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyPressed = event.getKeyCode();
        if (keyPressed == KeyEvent.KEYCODE_POWER) {
            Log.i("###", "Power button long click");
            Log.i(TAG, "Clicked: " + keyPressed);
            Toast.makeText(this, "wewkul", Toast.LENGTH_LONG).show();
            return true;
        } else
            return super.dispatchKeyEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            // Close every kind of system dialog
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
            updateUI();
            showToast("Starting capture!");
            pictureService.startCapturing(this);

            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("application/image");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, "satyabrat.me@gmail.com");
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Anti-Theft Alert");
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, coord);

            //emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(PictureCapturingServiceImpl.file.toString()));
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));

            //snapActivity();
            //startService(new Intent(MainActivity.this, CameraService.class));
            Toast.makeText(this, "LongPress Power Button", Toast.LENGTH_SHORT).show();
        }
    }*/
}
