package com.example.deepmatex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>
{
    private List<ChatMessage> userMessagesList;
    //private FirebaseAuth mAuth;
    private DatabaseReference chatsRef;


    public MessageAdapter (List<ChatMessage> userMessagesList)
    {
        this.userMessagesList = userMessagesList;
    }



    public class MessageViewHolder extends RecyclerView.ViewHolder
    {
        public TextView leftText, rightText;



        public MessageViewHolder(@NonNull View itemView)
        {
            super(itemView);

            leftText = (TextView) itemView.findViewById(R.id.leftText);
            rightText = (TextView) itemView.findViewById(R.id.rightText);
        }
    }




    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.msglist, viewGroup, false);

       // mAuth = FirebaseAuth.getInstance();

        return new MessageViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder messageViewHolder, int i)
    {
       // String messageSenderId = mAuth.getCurrentUser().getUid();
        ChatMessage messages = userMessagesList.get(i);

         String fromUser = messages.getMsgUser();
        //String fromMessageType = messages.getType();

       chatsRef = FirebaseDatabase.getInstance().getReference().child("Users");

        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });





        messageViewHolder.rightText.setVisibility(View.GONE);
        messageViewHolder.leftText.setVisibility(View.GONE);



            if (fromUser.equals("user"))
            {
                messageViewHolder.rightText.setVisibility(View.VISIBLE);

               // messageViewHolder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
               // messageViewHolder.senderMessageText.setTextColor(Color.BLACK);
                messageViewHolder.rightText.setText(messages.getMsgText());
            }
            else
            {
                //messageViewHolder.receiverProfileImage.setVisibility(View.VISIBLE);
                messageViewHolder.leftText.setVisibility(View.VISIBLE);

               // messageViewHolder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
               // messageViewHolder.receiverMessageText.setTextColor(Color.BLACK);
                messageViewHolder.leftText.setText(messages.getMsgText());
            }

    }




    @Override
    public int getItemCount()
    {
        return userMessagesList.size();
    }

}