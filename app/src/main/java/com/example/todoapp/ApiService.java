package com.example.todoapp;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("get-todos")
    Call<List<TodoItem>> getTodos();

    @DELETE("reset-all")
    Call<Void> resetAll();


    @DELETE("delete-todo/{id}")
    Call<Void> deleteTodoItem(@Path("id") String id);
}
