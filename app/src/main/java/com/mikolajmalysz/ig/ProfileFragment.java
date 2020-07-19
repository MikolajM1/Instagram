package com.mikolajmalysz.ig;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.util.Logger;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static androidx.constraintlayout.widget.Constraints.TAG;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ProfileFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ImageView imageView2;
    private ImageView imageView1;

    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private Button button5;

    private TextView textView1;
    private TextView textView2;

    String profileId;
    String profileName;
    String profileDesc;
    String profilePicId;
    String followersNumber;

    String postDesc;
    long likes;

    ArrayList<String> likedBy;
    String button2Text;
    boolean liked;

    int post = 0;

    ArrayAdapter arrayAdapter;

    ArrayList<String> posts;
    ArrayList<String> comments;

    Map<String, Object> postReadMap;

    FirebaseUser user;
    FirebaseFirestore db;

    private OnFragmentInteractionListener mListener;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_profile, container, false);

        Bundle bundle = getArguments();

        if (bundle != null){
            profileId = bundle.getString("profileId");
        }

        imageView1 = v.findViewById(R.id.profile_imageView1); //profile pic
        imageView2 = v.findViewById(R.id.profile_imageView2); //image

        button1 = v.findViewById(R.id.profile_button1); //follow
        button2 = v.findViewById(R.id.profile_button2); //like
        button3 = v.findViewById(R.id.profile_button3); //comment
        button4 = v.findViewById(R.id.profile_button4); //show description
        button5 = v.findViewById(R.id.profile_button5); //show bio

        textView1 = v.findViewById(R.id.profile_textView1); //profile name
        textView2 = v.findViewById(R.id.profile_textView2); //followers

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        delaySetImageViewSize();

        getUserData();

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                followUnfollow();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                like();
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComments();
            }
        });

        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDescription(postDesc);
            }
        });

        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDescription(profileDesc);
            }
        });

        imageView2.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                if (post < posts.size() - 1){
                    post++;
                    loadImage();
                }else{
                    Toast.makeText(getContext(), "You have reached the last element", Toast.LENGTH_SHORT).show();
                }
                Log.i("swipe", "Swiped Left");
            }
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                if (post < 1){
                    Toast.makeText(getContext(), "You have reached the first element", Toast.LENGTH_SHORT).show();
                }else{
                    post--;
                    loadImage();
                }
                Log.i("swipe", "Swiped Right");
            }
        });

        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    private void loadImage(){
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference photoReference= storageReference.child("posts/" + posts.get(post));

        final long ONE_MEGABYTE = 1024 * 1024;
        photoReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageView2.setImageBitmap(bmp);
                delaySetImageViewSize();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getContext(), "No Such file or Path found!!", Toast.LENGTH_LONG).show();
            }
        });

        DocumentReference docRef = db.collection("posts").document(posts.get(post));
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        postReadMap = new HashMap<>(document.getData());
                        postDesc = postReadMap.get("description").toString();
                        likes = Long.parseLong(postReadMap.get("likes").toString());
                        getComments();
                        button2.setText("LIKE" + " (" + likes + ")");
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        checkForLiking();
    }

    private void getComments(){
        comments = (ArrayList) postReadMap.get("comments");
        button3.setText("COMMENT" + " (" + comments.size() + ")");
    }

    private void getUserData(){
        DocumentReference docRef = db.collection("users").document(profileId);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        //Update user's arrayList to contain new image's ID
                        Map<String, Object> readMap = new HashMap<>(document.getData());
                        profileName = readMap.get("displayname").toString();
                        textView1.setText(profileName);
                        if (readMap.get("profile_picture") != null){
                            profilePicId = readMap.get("profile_picture").toString();
                        }
                        followersNumber = "Followers: " + readMap.get("followersNum").toString();
                        textView2.setText(followersNumber);
                        profileDesc = readMap.get("profileDesc").toString();
                        posts = (ArrayList) readMap.get("posts");
                        if (profilePicId != null){
                            loadProfilePicture();
                        }else{
                            Toast.makeText(getContext(), "No profile picture found", Toast.LENGTH_SHORT).show();
                        }
                        if (posts.size() == 0){
                            Toast.makeText(getContext(), "This profile does not contain any posts", Toast.LENGTH_SHORT).show();
                            button2.setVisibility(View.INVISIBLE);
                            button3.setVisibility(View.INVISIBLE);
                            button4.setVisibility(View.INVISIBLE);
                        }else{
                            loadImage();
                            checkForFollowing();
                            checkForLiking();
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void showDescription(String text){
        final Dialog d = new Dialog(getContext());
        d.setContentView(R.layout.dialog_text);
        d.setTitle("text");
        d.show();

        final TextView textView = d.findViewById(R.id.text_dialog_textView1);

        textView.setText(text);
    }

    private void showComments(){
        final Dialog d = new Dialog(getContext());
        d.setContentView(R.layout.dialog_comments);
        d.setTitle("comments");
        d.show();

        final EditText editText = d.findViewById(R.id.com_editText1);
        final Button button = d.findViewById(R.id.com_button1);
        final ListView listView = d.findViewById(R.id.com_listView);

        arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, comments);
        listView.setAdapter(arrayAdapter);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comments.add(editText.getText().toString());
                Map<String, Object> writeMap = new HashMap<>();
                writeMap.put("comments", comments);
                db.collection("posts").document(posts.get(post))
                        .set(writeMap, SetOptions.merge());
                getComments();
                arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, comments);
                listView.setAdapter(arrayAdapter);
            }
        });
    }

    private void followUnfollow(){
        //Check for already following
        if (button1.getText().toString().equals("FOLLOW")){
            follow();
        }else{
            unfollow();
        }
    }

    private void checkForFollowing(){
        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        Map<String, Object> readMap = new HashMap<>(document.getData());
                        ArrayList<String> arrayList = (ArrayList) readMap.get("followed");
                        if (arrayList != null){
                            if (arrayList.contains(profileId)){
                                button1.setText("UNFOLLOW");
                            }else{
                                button1.setText("FOLLOW");
                            }
                        }else{
                            Map<String, Object> writeMap = new HashMap<>();
                            writeMap.put("followed", new ArrayList<>());
                            db.collection("users").document(user.getUid())
                                    .set(writeMap, SetOptions.merge());
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void follow(){
        //update user's document to contain followed profile ID
        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        Map<String, Object> readMap = new HashMap<>(document.getData());
                        ArrayList<String> arrayList = (ArrayList) readMap.get("followed");
                        arrayList.add(profileId);
                        Map<String, Object> writeMap = new HashMap<>();
                        writeMap.put("followed", arrayList);
                        db.collection("users").document(user.getUid())
                                .set(writeMap , SetOptions.merge());
                        button1.setText("UNFOLLOW");
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        //update profile's document to increase followers count

        DocumentReference docRef2 = db.collection("users").document(profileId);
        docRef2.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        Map<String, Object> readMap = new HashMap<>(document.getData());
                        long followers = (long) readMap.get("followersNum");
                        followers++;
                        Map<String, Object> writeMap = new HashMap<>();
                        writeMap.put("followersNum", followers);
                        db.collection("users").document(profileId)
                                .set(writeMap , SetOptions.merge());
                        followersNumber = "Followers: " + followers;
                        textView2.setText(followersNumber);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void unfollow(){
        //update user's document to contain followed profile ID
        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        Map<String, Object> readMap = new HashMap<>(document.getData());
                        ArrayList<String> arrayList = (ArrayList) readMap.get("followed");
                        arrayList.remove(profileId);
                        Map<String, Object> writeMap = new HashMap<>();
                        writeMap.put("followed", arrayList);
                        db.collection("users").document(user.getUid())
                                .set(writeMap , SetOptions.merge());
                        button1.setText("FOLLOW");
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        //update profile's document to increase followers count

        DocumentReference docRef2 = db.collection("users").document(profileId);
        docRef2.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        Map<String, Object> readMap = new HashMap<>(document.getData());
                        long followers = (long) readMap.get("followersNum");
                        followers--;
                        Map<String, Object> writeMap = new HashMap<>();
                        writeMap.put("followersNum", followers);
                        db.collection("users").document(profileId)
                                .set(writeMap , SetOptions.merge());
                        followersNumber = "Followers: " +  followers;
                        textView2.setText(followersNumber);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void checkForLiking(){

        DocumentReference docRef = db.collection("posts").document(posts.get(post));
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        //Update user's arrayList to contain new image's ID
                        Map<String, Object> readMap = new HashMap<>(document.getData());
                        likedBy = (ArrayList) readMap.get("likedBy");
                        assert likedBy != null;
                        if (likedBy.contains(user.getUid())){
                            button2Text = "UNLIKE";
                            liked = true;
                        }else{
                            button2Text = "LIKE";
                            liked = false;
                        }
                        button2.setText(button2Text + " (" + likes + ")");
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void like(){
        if (!liked){
            likes++;
            Map<String, Object> writeMap = new HashMap<>();
            writeMap.put("likes", likes);
            likedBy.add(user.getUid());
            writeMap.put("likedBy", likedBy);
            db.collection("posts").document(posts.get(post))
                    .set(writeMap, SetOptions.merge());
            button2Text = "UNLIKE";
            liked = true;
        }else{
            likes--;
            Map<String, Object> writeMap = new HashMap<>();
            writeMap.put("likes", likes);
            likedBy.remove(user.getUid());
            writeMap.put("likedBy", likedBy);
            db.collection("posts").document(posts.get(post))
                    .set(writeMap, SetOptions.merge());
            button2Text = "LIKE";
            liked = false;
        }
        button2.setText(button2Text + " (" + likes + ")");
    }

    private void loadProfilePicture(){
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference photoReference= storageReference.child("profile/" + profilePicId);

        final long ONE_MEGABYTE = 1024 * 1024;
        photoReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageView1.setImageBitmap(bmp);
                delaySetImageViewSize();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getContext(), "No Such file or Path found!!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void delaySetImageViewSize(){
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                setImageViewSize();
            }
        }, 1);
    }

    private void setImageViewSize(){
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int screenHeight = metrics.heightPixels;

        Fragment f = getActivity().getSupportFragmentManager().findFragmentById(R.id.bottomFrameLayout);

        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        int viewsHeight = button2.getHeight() + imageView1.getHeight() + button1.getHeight();

        int usableHeight = screenHeight - f.getView().getHeight() - statusBarHeight - viewsHeight;

        Log.i("viewsHeight", String.valueOf(viewsHeight));
        Log.i("usableHeight", String.valueOf(usableHeight));

        imageView2.getLayoutParams().height = usableHeight;
        imageView2.requestLayout();
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
