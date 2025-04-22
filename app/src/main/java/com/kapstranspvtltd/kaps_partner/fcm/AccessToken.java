package com.kapstranspvtltd.kaps_partner.fcm;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kapstranspvtltd.kaps_partner.MyApplication;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AccessToken {
    private static String cachedToken = null;
    private static long tokenExpiry = 0;

    public static String getAccessToken() {
        // Return cached token if it's still valid
        if (cachedToken != null && !cachedToken.isEmpty() && System.currentTimeMillis() < tokenExpiry) {
            return cachedToken;
        }

        // Get new token
        try {
            return fetchNewToken().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CompletableFuture<String> fetchNewToken() {
        CompletableFuture<String> future = new CompletableFuture<>();
        String url = APIClient.baseUrl + "get_customer_app_firebase_access_token";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            String token = response.getString("token");
                            // Cache the token with 50 minutes expiry
                            cachedToken = token;
                            tokenExpiry = System.currentTimeMillis() + (50 * 60 * 1000);
                            future.complete(token);
                        } else {
                            future.completeExceptionally(new Exception("Failed to get access token"));
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                },
                error -> {
                    String errorMessage;
                    if (error instanceof NoConnectionError) {
                        errorMessage = "No internet connection";
                    } else if (error instanceof TimeoutError) {
                        errorMessage = "Request timed out";
                    } else if (error instanceof ServerError) {
                        errorMessage = "Server error";
                    } else {
                        errorMessage = "Error: " + error.getMessage();
                    }
                    future.completeExceptionally(new Exception(errorMessage));
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(MyApplication.mContext).addToRequestQueue(request);
        return future;
    }
}

/* Commented out original implementation using service account
public class AccessToken {
    private static final String FIREBASE_MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    public static String getAccessToken() {
        try {
            String jsonString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"vt-partner-8317b\",\n" +
                    // ... (rest of the JSON string)
                    "}\n";

            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));

            GoogleCredentials googleCredential = GoogleCredentials.fromStream(stream)
                    .createScoped(Collections.singletonList(FIREBASE_MESSAGING_SCOPE));

            googleCredential.refresh();
            return googleCredential.getAccessToken().getTokenValue();

        } catch (IOException e) {
            return null;
        }
    }
}
*/