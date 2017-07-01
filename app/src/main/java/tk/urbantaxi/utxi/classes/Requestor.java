package tk.urbantaxi.utxi.classes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static tk.urbantaxi.utxi.classes.Constants.SHARED_PREFERENCE;
import static tk.urbantaxi.utxi.classes.Constants.URL;

/**
 * Created by steph on 6/15/2017.
 */

public class Requestor {

    private String url;
    private Boolean isRunning = false;
    private Map<String, Object> param;
    private Context context;
    private Boolean asynchronus = false;
    private Integer PAGE = null;

    public Requestor(String url, Map<String, Object> param, Context context){
        this.url = URL + url;
        this.param = param;
        this.context = context;
    }

    public void execute(){
        if(PAGE != null){
            param.put("page", PAGE);
        }

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCE, MODE_PRIVATE);
        String userDetails = prefs.getString("userDetails", null);
        if (userDetails != null) {
            try {
                JSONObject userObject = new JSONObject(userDetails);
                String username = userObject.getString("username");
                String password = userObject.getString("password");
                param.put("username", username);
                param.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Boolean network = isNetworkAvailable();
        if (network == true) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wInfo = wifiManager.getConnectionInfo();
            String macAddress = wInfo.getMacAddress();
            if(macAddress.equals("02:00:00:00:00:00")){
                macAddress = getWifiMacAddress();
            }
            param.put("macaddress", macAddress);
            param.put("type", "user");
            param.put("fcmtoken", FirebaseInstanceId.getInstance().getToken());
            if (isRunning == true && asynchronus == true) {
                new Ajaxer().execute();
            }else{
                new Ajaxer().execute();
            }
        } else {
            onNetworkError();
        }
    }

    public void onNetworkError(){
        Toast.makeText(context, "Something went wrong.", Toast.LENGTH_LONG).show();
    }

    public void preExecute(){

    }

    public void postExecute(Boolean cancelled, String result){

    }

    public void cancelled(){
        Toast.makeText(context, "Something went wrong.", Toast.LENGTH_LONG);
    }

    public class Ajaxer extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isRunning = true;
            preExecute();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            isRunning = false;
            cancelled();
            postExecute(true, "");
        }

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                byte[] postDataBytes = urlParams(param);
                java.net.URL urlj = new URL(url);
                connection = (HttpURLConnection) urlj.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                connection.setDoOutput(true);
                connection.getOutputStream().write(postDataBytes);
                connection.setConnectTimeout(30000);
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                String finalJson = buffer.toString().trim();
                return finalJson;
            } catch (MalformedURLException e) {
                cancel(true);
            } catch (IOException e) {

                cancel(true);
            } finally {
                if(connection != null) {
                    connection.disconnect();
                }
                try {
                    if(reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    cancel(true);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            isRunning = false;
            postExecute(false, s);
        }
    }

    public static byte[] urlParams(Map<String, Object> params) throws UnsupportedEncodingException {
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");
        return postDataBytes;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void showDialog(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public static String getWifiMacAddress() {
        try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)){
                    continue;
                }

                byte[] mac = intf.getHardwareAddress();
                if (mac==null){
                    return "";
                }

                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public void setPage(Integer page){
        this.PAGE = page;
    }

    public void setParam(Map<String, Object> params){
        this.param = params;
    }

    public void setAsynchronus(Boolean bool){
        this.asynchronus = bool;
    }
}
