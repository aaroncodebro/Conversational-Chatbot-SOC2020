package com.example.deepmatex;

import android.text.Editable;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface JsonPlaceHolderApi {

    @GET("reply/{query}")
    Call<Reply> getReply(@Path("query") String query);
}
