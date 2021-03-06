package tk.urbantaxi.utxi.classes;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


/**
 * Created by steph on 6/17/2017.
 */

public class Firebase extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        Log.e("TEST", "TEST");
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.e("Google", "Refreshed Token: " + refreshedToken);
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        //sendRegistrationToServer(refreshedToken);
    }
}
