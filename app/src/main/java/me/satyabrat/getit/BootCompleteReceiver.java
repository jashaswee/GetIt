package me.satyabrat.getit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;

public class BootCompleteReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ) {
            Log.i(BroadcastReceiver.class.getSimpleName(), "Boot Complete...");
            //Intent i = new Intent(context, MainActivity.class);
            //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //context.startActivity(i);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String loc = preferences.getString("Location", "");
            if (!loc.equalsIgnoreCase("")) {
                //if (!PictureCapturingServiceImpl.file.toString().equalsIgnoreCase(null)) {
                //Log.e(TAG, "file://" + PictureCapturingServiceImpl.file.toString());
                Log.e("BootupReceiver", "thisruns");
                BackgroundMail.newBuilder(context)
                        .withUsername("getit.app.antitheft@gmail.com")
                        .withPassword("thuglife69")
                        .withMailto("satya.bshs2014@gmail.com")
                        .withType(BackgroundMail.TYPE_PLAIN)
                        .withSubject("GetIT Anti-Theft Alert")
                        .withBody(loc)
                        //.withAttachments("file://" + PictureCapturingServiceImpl.file.toString())
                        //.withAttachments("file:///storage/emulated/0/0_pic.jpg")
                        .send();
            }
            }

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isConnected = activeNetInfo != null && activeNetInfo.isConnectedOrConnecting();
        if (isConnected)
            Log.i("NET", "connected" +isConnected);
        else Log.i("NET", "not connected" +isConnected);

    }
}
