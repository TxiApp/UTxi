package tk.urbantaxi.utxi;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

import tk.urbantaxi.utxi.classes.Requestor;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static tk.urbantaxi.utxi.classes.Constants.USER_PROFILES;
import static tk.urbantaxi.utxi.classes.Constants.VEHICLE_URL;

public class Driver extends AppCompatActivity implements View.OnClickListener {
    private TextView tvName;
    private TextView tvCompany;
    private ImageView ivImage;
    private TextView tvModel;
    private TextView tvPlate;
    private ImageView ivVehicleImage;
    private Button btnBook;
    private Button btnBack;
    private Requestor requestor;
    private Intent intent;
    private ProgressDialog dialog;
    private EditText etDestination;
    public CountDownTimer countDownTimer;
    public Integer requestId;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        tvName = (TextView)findViewById(R.id.tvName);
        tvCompany = (TextView)findViewById(R.id.tvCompany);
        ivImage = (ImageView)findViewById(R.id.ivImage);
        tvModel = (TextView)findViewById(R.id.tvModel);
        tvPlate = (TextView)findViewById(R.id.tvPlate);
        ivVehicleImage = (ImageView)findViewById(R.id.ivVehicleImage);
        btnBook = (Button)findViewById(R.id.btnBook);
        btnBack = (Button)findViewById(R.id.btnBack);
        etDestination = (EditText)findViewById(R.id.etDestination);

        intent = getIntent();
        tvName.setText(intent.getStringExtra("name"));
        tvCompany.setText(intent.getStringExtra("company"));
        Picasso.with(this).load(USER_PROFILES + intent.getStringExtra("image")).into(ivImage);
        tvModel.setText(intent.getStringExtra("model"));
        tvPlate.setText(intent.getStringExtra("plate"));
        Picasso.with(this).load(VEHICLE_URL + intent.getStringExtra("vehicleImage")).into(ivVehicleImage);

        dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Loading...");

        btnBack.setOnClickListener(this);
        btnBook.setOnClickListener(this);
        etDestination.setOnClickListener(this);

        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(
                mMessageReceiver, new IntentFilter("Request"));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnBack:
                finish();
                break;
            case R.id.btnBook:
                Map<String, Object> param = new LinkedHashMap<>();
                param.put("session_id", intent.getIntExtra("sessionId", 1));
                param.put("latitude", intent.getDoubleExtra("pickupLat", 1));
                param.put("longitude", intent.getDoubleExtra("pickupLng", 1));
                param.put("address", intent.getStringExtra("address"));
                param.put("destination", etDestination.getText().toString());
                requestor = new Requestor("request", param, this){
                    @Override
                    public void preExecute(){
                        dialog.show();
                    }

                    @Override
                    public void onNetworkError(){
                        showDialog("Network Error", "Please check your internet connection.");
                    }

                    @Override
                    public void postExecute(Boolean cancelled, String result){
                        if(cancelled){
                            dialog.dismiss();
                            showDialog("Ooops...", "Something went wrong");
                        }else{
                            try {
                                JSONObject object = new JSONObject(result);
                                String res = object.getString("result");
                                if(res.equals("success")){
                                    requestId = object.getInt("request_id");
                                    dialog.setMessage("Please wait for the driver to respond");
                                    countDownTimer = new CountDownTimer(20000, 1000) {

                                        public void onTick(long millisUntilFinished) {

                                        }

                                        public void onFinish() {
                                            Map<String, Object> param = new LinkedHashMap<>();
                                            param.put("id", requestId);
                                            Requestor failedToRespondRequest = new Requestor("failedrequest", param, getApplicationContext()){
                                                @Override
                                                public void onNetworkError(){
                                                    Log.e("Error", "No internet connection.");
                                                }

                                                @Override
                                                public void postExecute(Boolean cancelled, String result){
                                                    dialog.dismiss();
                                                    Toast.makeText(getApplicationContext(), "The taxi driver did not respond to your request.", Toast.LENGTH_LONG).show();
                                                }
                                            };
                                            failedToRespondRequest.execute();
                                        }
                                    }.start();
                                }else{
                                    dialog.dismiss();
                                    showDialog("", "Taxi has already been reserved.");
                                }
                            } catch (JSONException e) {
                                dialog.dismiss();
                                showDialog("Ooops...", "Something went wrong");
                            }
                        }
                    }
                };

                requestor.execute();
                break;
            case R.id.etDestination:
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .build(this);
                    startActivityForResult(intent, 1);
                } catch (GooglePlayServicesRepairableException e) {
                    Log.e("Repair", "repair");
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                    Log.e("Repair", "repair2");
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.e("Place", "Place: " + place.getAddress());
                etDestination.setText(place.getAddress());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("Status", status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(requestId == Integer.valueOf(intent.getStringExtra("id"))){
                Log.e("BROADCAST", intent.getStringExtra("id"));
                switch (intent.getStringExtra("action")){
                    case "reject":
                        countDownTimer.cancel();
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Taxi driver has rejected your request.", Toast.LENGTH_LONG).show();
                        break;
                    case "accept":
                        countDownTimer.cancel();
                        dialog.dismiss();
                        Intent intentBook = new Intent(getApplicationContext(), Book.class);
                        intentBook.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intentBook);
                }
            }
        }
    };
}
