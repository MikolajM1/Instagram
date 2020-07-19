package com.mikolajmalysz.ig;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private Button button1;
    private Button button2;
    private EditText editText;
    private EditText editText2;
    private EditText editText3;
    private EditText editText4;
    private Button button3;


    private FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseUser user;

    String birthdate;

    private DatePickerDialog.OnDateSetListener dataDialogListener;

    private OnFragmentInteractionListener mListener;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_register, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        button1 = v.findViewById(R.id.rbutton);
        button2 = v.findViewById(R.id.rbutton2);

        editText = v.findViewById(R.id.reditText); //Name
        editText2 = v.findViewById(R.id.reditText2); //E-mail
        editText3 = v.findViewById(R.id.reditText3); //Password
        editText4 = v.findViewById(R.id.reditText4); //Confirm password
        button3 = v.findViewById(R.id.rbutton3);  //Date of birth

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText3.getText().toString().equals(editText4.getText().toString())){
                    if (dates()){
                        emailUse();
                    }else{
                        Toast.makeText(getContext(), "Date of birth is under 13", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getContext(), "Passwords are different", Toast.LENGTH_SHORT).show();
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getContext(), LoginHolderActivity.class);
                startActivity(i);
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDate();
            }
        });

        return v;
    }

    public void register(){
        mAuth.createUserWithEmailAndPassword(editText2.getText().toString(), editText3.getText().toString())
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("register", "createUserWithEmail:success");
                            user = mAuth.getCurrentUser();
                            Map<String, Object> writeMap = new HashMap<>();
                            writeMap.put("displayname", editText.getText().toString());
                            writeMap.put("birth", birthdate);
                            writeMap.put("followed", new ArrayList<>());
                            Log.i("writeMap", "done");
                            db.collection("users").document(user.getUid()).set(writeMap);
                            sendEmailVerification();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("register", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                        // ...
                    }
                });


    }

    private void displayName(){
        CollectionReference users = db.collection("users");
        Query query = users.whereEqualTo("displayname", editText.getText().toString());
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    int i = 0;
                    for (QueryDocumentSnapshot document : task.getResult()){
                        i++;
                    }
                    if (i == 0){
                        register();
                    }else{
                        Toast.makeText(getContext(), "Display name is already in use", Toast.LENGTH_SHORT).show();
                    }
                    Log.i("query", "Task successful");
                }else{
                    Log.i("query", "Task not successful");
                }
            }
        });
    }

    private void emailUse(){
        mAuth.fetchSignInMethodsForEmail(editText2.getText().toString()).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                if (task.getResult().getSignInMethods().isEmpty()){
                    Log.i("Check for email use", "not used at all");
                    displayName();
                }else{
                    Toast.makeText(getContext(), "Email address is already in use", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean dates(){
        SimpleDateFormat sdformat = new SimpleDateFormat("dd/MM/yyyy");
        Date d1 = new Date();
        try {
            d1 = sdformat.parse(birthdate);
        }catch (Exception e){
            Log.i("dates exception", e.toString());
            Toast.makeText(getContext(), "Please provide a birth date", Toast.LENGTH_SHORT).show();
        }

        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -13);
        Date c1 = c.getTime();

        if (c1.after(d1)){
            return true;
        }else{
            Toast.makeText(getContext(), "Date is less than 13 years old", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void loadDate(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        dataDialogListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                birthdate = day + "/" + month + "/" + year;
                Log.d("onDate", birthdate);
            }
        };
        DatePickerDialog dialog = new DatePickerDialog(getContext(), android.R.style.Theme_DeviceDefault_DayNight, dataDialogListener, year, month, day);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        dialog.show();
    }

    public void sendEmailVerification(){
        if (user != null){
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d("email verification", "Email verification sent.");
                                mAuth.signOut();

                                Toast.makeText(getContext(), "We've sent a verification link to this email address. Please sign in when you verified your email.", Toast.LENGTH_LONG).show();
                                Intent i = new Intent(getContext(), LoginHolderActivity.class);
                                i.putExtra("email", editText2.getText().toString());
                                i.putExtra("passwd", editText3.getText().toString());
                                startActivity(i);

                            }
                        }
                    });
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
