package com.kapstranspvtltd.kaps_partner.goods_driver_activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.adapters.FAQAdapter;
import com.kapstranspvtltd.kaps_partner.models.FAQ;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoodsFAQSActivity extends AppCompatActivity {
    private RecyclerView recyclerViewFaqs;
    private FAQAdapter faqAdapter;
    private long categoryId;

    PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_faqsactivity);

        // Setup toolbar
preferenceManager = new PreferenceManager(this);

        // Get category ID from intent
        categoryId = getIntent().getLongExtra("category_id", 1);

        // Initialize RecyclerView
        recyclerViewFaqs = findViewById(R.id.recyclerViewFaqs);
        recyclerViewFaqs.setLayoutManager(new LinearLayoutManager(this));
        faqAdapter = new FAQAdapter(this);
        recyclerViewFaqs.setAdapter(faqAdapter);

        // Load FAQs
        loadFAQs();
    }

    private void loadFAQs() {
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("category_id", categoryId);
            jsonBody.put("driver_unique_id", driverId);
            jsonBody.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                APIClient.baseUrl + "get_faqs_by_category",
                jsonBody,
                response -> {
                    try {
                        JSONArray faqsArray = response.getJSONArray("faqs");
                        List<FAQ> faqs = new ArrayList<>();

                        for (int i = 0; i < faqsArray.length(); i++) {
                            JSONObject faqObject = faqsArray.getJSONObject(i);
                            FAQ faq = new FAQ(
                                    faqObject.getInt("faq_id"),
                                    faqObject.getString("question"),
                                    faqObject.getString("answer"),
                                    faqObject.getDouble("time_at"),
                                    faqObject.getInt("category_id")
                            );
                            faqs.add(faq);
                        }

                        faqAdapter.setFaqs(faqs);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing FAQs", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    handleError(error);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                // Add any other required headers
                return headers;
            }
        };

        // Add request to queue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void handleError(VolleyError error) {
        String message;
        // Check if there's a network response
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;

            switch (statusCode) {

                case 404:
                    message = "No FAQS found";
                    break;
                case 400:
                    message = "Bad request";
                    break;
                case 500:
                    message = "Server error";
                    break;
                default:
                    message = "Error ";
                    break;
            }
        } else {
            // Handle cases where there's no network response
            if (error instanceof NetworkError) {
                message = "No internet connection";
            } else if (error instanceof TimeoutError) {
                message = "Request timed out";
            } else if (error instanceof ServerError) {
                message = "Server error";
            } else {
                message = "Error fetching plan details";
            }
        }
        showError(message);

    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}