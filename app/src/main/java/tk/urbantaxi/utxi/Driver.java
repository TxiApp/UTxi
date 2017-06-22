package tk.urbantaxi.utxi;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Driver extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);
    }
    public void gobook (View view){
        Intent intent = new Intent (this, Book.class);
        startActivity(intent);
    }
    public void goback (View view){
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
    }
}
