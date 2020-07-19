package com.mikolajmalysz.ig;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class HomeFragment extends Fragment {

    private Button button1;
    private Button button2;
    private Button button3;
    private Button buttonSignOut;

    private TextView textView1;
    private ImageView imageView1;

    View v;

    ArrayList<String> posts = new ArrayList<>();

    FirebaseUser user;
    FirebaseFirestore db;

    String postDesc;
    String postProfile;
    long likes;

    ArrayList<String> likedBy;
    String button1Text;
    boolean liked;

    int post = 0;

    ArrayAdapter arrayAdapter;

    ArrayList<String> comments;

    Map<String, Object> postReadMap;

    private OnFragmentInteractionListener mListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null){
            Toast.makeText(getContext(), "Swipe left to show next post, right to show previous", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_home, container, false);

        imageView1 = v.findViewById(R.id.home_imageView1);

        textView1 = v.findViewById(R.id.home_textView1);

        button1 = v.findViewById(R.id.home_button1); //like
        button2 = v.findViewById(R.id.home_button2); //desc
        button3 = v.findViewById(R.id.home_button3); //comments


        buttonSignOut = v.findViewById(R.id.sign_out_button);

        delaySetImageViewSize();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                like();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDescription(postDesc);
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComments();
            }
        });

        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getContext(), LoginHolderActivity.class);
                startActivity(intent);
            }
        });

        imageView1.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                if (post < posts.size() - 1){
                    post++;
                    loadImage();
                }else{
                    loadRandom();
                    Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            posts = removeDuplicates(posts);
                            if (post < posts.size() - 1){
                                post++;
                                loadImage();
                            }else{
                                Toast.makeText(getContext(), "You have reached the last item", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, 1000);
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

        textView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                ProfileFragment profileFragment = new ProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putString("profileId", postProfile);
                profileFragment.setArguments(bundle);
                transaction.add(R.id.frameLayout, profileFragment).replace(R.id.frameLayout, profileFragment).addToBackStack(null).commit();
            }
        });

        getFollowed();

        delayLoadImages();

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (posts.size() == 0){
                    loadRandom();
                    delayLoadImages();
                    Log.i("loadRandom", "true");
                }
            }
        }, 2000);

        return v;
    }

    public static <T> ArrayList<T> removeDuplicates(ArrayList<T> list)
    {

        // Create a new LinkedHashSet

        // Add the elements to set
        Set<T> set = new LinkedHashSet<>(list);

        // Clear the list
        list.clear();

        // add the elements of set
        // with no duplicates to the list
        list.addAll(set);

        // return the list
        return list;
    }


    private void delayLoadImages(){
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(posts.size() > 0){
                    loadImage();
                }
                Log.i("size of posts", String.valueOf(posts.size()));
            }
        }, 2000);
    }

    private void loadRandom(){
        final ArrayList<String> userNames = new ArrayList<>();
        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Map<String, Object> readMap;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                if (document.get("posts") != null){
                                    userNames.add(document.getId());
                                }
                            }
                            getPosts(userNames);
                            Log.i("userNames", userNames.toString());
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
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
        }, 100);
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

        int viewsHeight = textView1.getHeight() + button1.getHeight();

        ViewGroup.LayoutParams imageView1LayoutParams = imageView1.getLayoutParams();
        int imageViewMarginTop = ((ConstraintLayout.LayoutParams) imageView1LayoutParams).topMargin;
        ViewGroup.LayoutParams textView1LayoutParams = textView1.getLayoutParams();
        int textViewMarginTop = ((ConstraintLayout.LayoutParams) textView1LayoutParams).topMargin;

        int paddingHeight = textView1.getPaddingTop() + imageViewMarginTop + textViewMarginTop;

        int usableHeight = screenHeight - f.getView().getHeight() - statusBarHeight - viewsHeight - paddingHeight;

        Log.i("viewsHeight", String.valueOf(viewsHeight));
        Log.i("paddingHeight", String.valueOf(paddingHeight));
        Log.i("usableHeight", String.valueOf(usableHeight));

        imageView1.getLayoutParams().height = usableHeight;
        imageView1.requestLayout();
    }

    private void loadImage(){
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference photoReference= storageReference.child("posts/" + posts.get(post));

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
                        likes = Long.valueOf(postReadMap.get("likes").toString());
                        postProfile = postReadMap.get("profile").toString();
                        getComments();
                        button1.setText("LIKE" + " (" + likes + ")");
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });



        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                DocumentReference docRef2 = db.collection("users").document(postProfile);
                docRef2.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                Map <String, Object> readMap = new HashMap<>(document.getData());

                                textView1.setText(readMap.get("displayname").toString());
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });
            }
        }, 1000);

        checkForLiking();
    }

    private void getComments(){
        comments = (ArrayList) postReadMap.get("comments");
        button3.setText("COMMENT" + " (" + comments.size() + ")");
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

    private void showDescription(String text){
        final Dialog d = new Dialog(getContext());
        d.setContentView(R.layout.dialog_text);
        d.setTitle("text");
        d.show();

        final TextView textView = d.findViewById(R.id.text_dialog_textView1);

        textView.setText(text);
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
                        if (likedBy.contains(user.getUid())){
                            button1Text = "UNLIKE";
                            liked = true;
                        }else{
                            button1Text = "LIKE";
                            liked = false;
                        }
                        button1.setText(button1Text + " (" + likes + ")");
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
            button1Text = "UNLIKE";
            liked = true;
        }else{
            likes--;
            Map<String, Object> writeMap = new HashMap<>();
            writeMap.put("likes", likes);
            likedBy.remove(user.getUid());
            writeMap.put("likedBy", likedBy);
            db.collection("posts").document(posts.get(post))
                    .set(writeMap, SetOptions.merge());
            button1Text = "LIKE";
            liked = false;
        }
        button1.setText(button1Text + " (" + likes + ")");
    }

    private void getFollowed(){
        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        //Update user's arrayList to contain new image's ID
                        Map<String, Object> readMap = new HashMap<>(document.getData());
                        ArrayList<String> followed = (ArrayList) readMap.get("followed");
                        if (followed != null){
                            getPosts(followed);
                        }else{
                            Toast.makeText(getContext(), "No followed user's found", Toast.LENGTH_SHORT).show();
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

    private void getPosts(ArrayList<String> followed){
        for (int i = 0; i<followed.size(); i++){
            DocumentReference docRef = db.collection("users").document(followed.get(i));
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            //Update user's arrayList to contain new image's ID
                            Map<String, Object> readMap = new HashMap<>(document.getData());
                            ArrayList<String> followedPosts = (ArrayList) readMap.get("posts");
                            Log.i("followedPosts", followedPosts.toString());
                            posts.addAll(followedPosts);
                            Log.i("Posts", posts.toString());
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        }
        Log.i("posts's size", String.valueOf(posts.size()));
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

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
