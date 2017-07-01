package tk.urbantaxi.utxi.classes;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static tk.urbantaxi.utxi.classes.Constants.SHARED_PREFERENCE;

/**
 * Created by steph on 7/1/2017.
 */

public class MessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage){
        String mode = remoteMessage.getData().get("mode");
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCE, MODE_PRIVATE);
        String userDetails = prefs.getString("userDetails", null);
        if (userDetails != null) {
            JSONObject userObject = null;
            try {
                userObject = new JSONObject(userDetails);
                String username = userObject.getString("username");
                if(username.equals(remoteMessage.getData().get("to"))){
                    switch (mode) {
                        case "rejectrequest":
                            if(isActivityOnForeground(getPackageName()+ ".Driver")){
                                Log.e("Foregroud", "TRUE");
                                Intent intent = new Intent("Request");
                                intent.putExtra("id", remoteMessage.getData().get("id"));
                                intent.putExtra("action", "reject");
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                            }
                            break;
                        case "acceptrequest":
                            if(isActivityOnForeground(getPackageName()+ ".Driver")){
                                Log.e("Foregroud", "TRUE");
                                Intent intent = new Intent("Request");
                                intent.putExtra("id", remoteMessage.getData().get("id"));
                                intent.putExtra("action", "accept");
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                            }
                            break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isActivityOnForeground(String myPackage) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getClassName().equals(myPackage);
    }
}
