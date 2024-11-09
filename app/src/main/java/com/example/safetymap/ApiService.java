package com.example.safetymap;

import com.example.safetymap.Warning;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @GET("warnings/")
    Call<List<Warning>> getWarnings();

    @POST("warnings/")
    Call<Warning> createWarning(@Body Warning warning);
}