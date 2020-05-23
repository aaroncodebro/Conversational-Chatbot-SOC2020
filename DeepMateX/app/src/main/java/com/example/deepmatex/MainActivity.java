package com.example.deepmatex;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import android.view.View;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    EditText editText;
    RelativeLayout addBtn;
    DatabaseReference ref;
    Boolean flagFab = true;

    private JsonPlaceHolderApi jsonPlaceHolderApi;

    private final List<ChatMessage> messagesList = new ArrayList<>();
    private MessageAdapter messageAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.233.167.213:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},1);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        editText = (EditText) findViewById(R.id.editText);
        addBtn = (RelativeLayout) findViewById(R.id.addBtn);

        messageAdapter = new MessageAdapter(messagesList);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        //linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(messageAdapter);

        ref = FirebaseDatabase.getInstance().getReference();
        ref.keepSynced(true);





        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(flagFab==true)
                {
                    getSpeechInput(view);

                }

                String message = editText.getText().toString().trim();

                if (!message.equals("")) {

                    ChatMessage chatMessage = new ChatMessage(message, "user");
                    ref.child("chat").push().setValue(chatMessage);
                    getResponse(message);

                }

                editText.setText("");

            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = (ImageView)findViewById(R.id.fab_img);
                Bitmap img = BitmapFactory.decodeResource(getResources(),R.drawable.ic_send_white_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(),R.drawable.ic_mic_white_24dp);


                if (s.toString().trim().length()!=0 && flagFab){
                    ImageViewAnimatedChange(MainActivity.this,fab_img,img);
                    flagFab=false;

                }
                else if (s.toString().trim().length()==0){
                    ImageViewAnimatedChange(MainActivity.this,fab_img,img1);
                    flagFab=true;

                }


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });




    }
    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }
    @Override
    protected void onStart()
    {
        super.onStart();

        ref.child("chat")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s)
                    {

                        ChatMessage messages = dataSnapshot.getValue(ChatMessage.class);

                        messagesList.add(messages);
                        if(messages.getMsgText().equals(""))
                        {
                            Toast.makeText(MainActivity.this,"fuck",Toast.LENGTH_SHORT).show();
                            }



                        messageAdapter.notifyDataSetChanged();

                        recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void getResponse(String query)
    {

        Call<Reply> call = jsonPlaceHolderApi.getReply(query);

        call.enqueue(new Callback<Reply>() {
            @Override
            public void onResponse(Call<Reply> call, Response<Reply> response) {

                if(!response.isSuccessful())
                {

                    return ;
                }

                Reply reply = response.body();


                String content = reply.getReply() ;
                ChatMessage chatMessage = new ChatMessage(content, "bot");
                ref.child("chat").push().setValue(chatMessage);





            }

            @Override
            public void onFailure(Call<Reply> call, Throwable t) {


            }
        });
    }
    public void getSpeechInput(View view)
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        if(intent.resolveActivity(getPackageManager())!=null)
        {
            startActivityForResult(intent, 10);
        }
        else{
            Toast.makeText(this, "Your Device doesnt support Speech Input",Toast.LENGTH_SHORT).show();
        }
;

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 10:
                if(resultCode == RESULT_OK && data != null)
                {
                 ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                 editText.setText(result.get(0));

                }
                break;
        }


    }


}

