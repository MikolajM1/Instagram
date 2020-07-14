package com.mikolajmalysz.ig;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginHolderActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    String email;
    String passwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_holder);

        mAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();

        if (intent.getStringExtra("email") != null){
            email = intent.getStringExtra("email");
            passwd = intent.getStringExtra("passwd");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        FirebaseAuth.getInstance().signOut(); //Temporary TODO Remove
//        if (currentUser != null){
//            Intent i = new Intent(getApplicationContext(), MainActivity.class);
//            startActivity(i);
//        }else{
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            LoginFragment loginFragment = new LoginFragment();
            if (email != null && passwd != null){
                Bundle b = new Bundle();
                b.putString("email", email);
                b.putString("passwd", passwd);
                loginFragment.setArguments(b);
            }
            transaction.add(R.id.loginFrameLayout, loginFragment).addToBackStack(null).commit();
//        }
    }
}
