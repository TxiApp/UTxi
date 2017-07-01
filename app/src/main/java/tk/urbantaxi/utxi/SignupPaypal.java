package tk.urbantaxi.utxi;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Map;

import tk.urbantaxi.utxi.classes.Requestor;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SignupPaypal extends AppCompatActivity implements View.OnClickListener {
    private Button btnSubmit;
    private Requestor requestor;
    private Map<String, Object> param;
    private Intent intent;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_paypal);
        intent = getIntent();
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        param = new LinkedHashMap<>();
        String fullname = intent.getStringExtra("fullname");
        String username = intent.getStringExtra("username");
        String email = intent.getStringExtra("email");
        String mobile = intent.getStringExtra("mobile");
        String password = intent.getStringExtra("password");
        param.put("fullname",fullname);
        param.put("username",username);
        param.put("email",email);
        param.put("mobile",mobile);
        param.put("password",password);
        requestor = new Requestor("signup", param, this){
            @Override
            public void postExecute(Boolean cancelled, String result){
                Toast.makeText(getApplicationContext(), "Successfully Sign Up an Account", Toast.LENGTH_LONG).show();
                Intent intent2 = new Intent(getApplicationContext(),Login.class);
                startActivity(intent2);
            }
        };
        requestor.execute();
    }
}
