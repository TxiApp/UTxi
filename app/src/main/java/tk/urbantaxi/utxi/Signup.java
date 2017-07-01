package tk.urbantaxi.utxi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Map;

import tk.urbantaxi.utxi.classes.Requestor;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Signup extends AppCompatActivity implements View.OnClickListener {
    private ProgressDialog dialog;
    private EditText etFullname;
    private EditText etUsername;
    private EditText etEmail;
    private EditText etMobile;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnNext;
    private Requestor requestor;
    private Map<String, Object> param;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        etFullname = (EditText) findViewById(R.id.etFullname);
        etUsername = (EditText) findViewById(R.id.etUsername);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etMobile = (EditText) findViewById(R.id.etMobile);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etConfirmPassword = (EditText) findViewById(R.id.etConfirmPassword);
        btnNext = (Button) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);
    }
    public void showDialog(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
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
    @Override
    public void onClick(View v) {
        String fullname = etPassword.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String conf = etConfirmPassword.getText().toString().trim();
        Boolean er;
        if(fullname.equals("")||username.equals("")||mobile.equals("")||email.equals("")||pass.equals("")||conf.equals("")){
            showDialog("", "Please fill up every text field");
        }
        else if(!pass.equals(conf)){
            showDialog("Password Does not match", "Please re-type your Password and Confirm Password");
        }else{
            Intent intent = new Intent(this, SignupPaypal.class);
            intent.putExtra("fullname",etFullname.getText().toString());
            intent.putExtra("username",etUsername.getText().toString());
            intent.putExtra("email",etEmail.getText().toString());
            intent.putExtra("mobile",etMobile.getText().toString());
            intent.putExtra("password",etPassword.getText().toString());
            startActivity(intent);
        }
    }
}
