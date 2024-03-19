package com.example.todoapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.net.URISyntaxException;
import java.util.List;
import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TodoAdapter adapter;
    private ApiService apiService;
    private MainViewModel viewModel;
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();



        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://todoapp-au7f.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        apiService = retrofit.create(ApiService.class);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getTodoItemsLiveData().observe(this, todoItems -> {
            adapter = new TodoAdapter(todoItems, MainActivity.this,
                    new TodoAdapter.OnItemClickListener() {
                        @Override
                        public void onEditClick(TodoItem item) {
                            showEditDialog(item);
                        }

                        @Override
                        public void onDeleteClick(TodoItem item) {
                            showDeleteConfirmationDialog(item);
                        }
                    });
            recyclerView.setAdapter(adapter);
        });


        findViewById(R.id.button_reset_all_notes).setOnClickListener(view -> resetAllNotes());

        connectToSocketServer();
        setupSocketListeners();
        fetchTodoItems();
    }

    private void connectToSocketServer() {
        try {
            mSocket = IO.socket("https://todoapp-au7f.onrender.com/"); // Ensure this matches your actual API base URL
            mSocket.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupSocketListeners() {
        mSocket.on("todoCreated", args -> runOnUiThread(this::fetchTodoItems));
        mSocket.on("todoDeleted", args -> runOnUiThread(this::fetchTodoItems));
    }

    private void fetchTodoItems() {
        apiService.getTodos().enqueue(new Callback<List<TodoItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<TodoItem>> call, @NonNull Response<List<TodoItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    viewModel.setTodoItemsLiveData(response.body());
                } else {
                    Log.e("TAG", "Error fetching todo items: " + response.errorBody());
                    Toast.makeText(MainActivity.this, "Failed to fetch todo items", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TodoItem>> call, @NonNull Throwable t) {
                Log.e("MainActivity", "Network error while fetching todo items", t);
                Log.e("TAG", "Error fetching todo items: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Failed to fetch todo items", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @SuppressLint("NotifyDataSetChanged")
    private void showEditDialog(TodoItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Todo");
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_todo, null);
        EditText editText = dialogView.findViewById(R.id.editTextTodoTitle);
        editText.setText(item.getTitle());
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String updatedTitle = editText.getText().toString().trim();
            if (!updatedTitle.isEmpty()) {
                item.setTitle(updatedTitle);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(MainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteConfirmationDialog(TodoItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Todo");
        builder.setMessage("Are you sure you want to delete this todo?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Call the API to delete the todo item
            apiService.deleteTodoItem(item.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        // Item deleted successfully, refresh the list or notify user
                        fetchTodoItems(); // Refresh the todo items list
                        Toast.makeText(MainActivity.this, "Todo item deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        // Handle failure, e.g., item not found or server error
                        Toast.makeText(MainActivity.this, "Failed to delete todo item", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Toast.makeText(MainActivity.this, "Network error or server is unreachable", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void resetAllNotes() {
        apiService.resetAll().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "All todo items reset successfully", Toast.LENGTH_SHORT).show();
                    fetchTodoItems();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to reset all todo items", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to reset all todo items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
        }
    }
}
