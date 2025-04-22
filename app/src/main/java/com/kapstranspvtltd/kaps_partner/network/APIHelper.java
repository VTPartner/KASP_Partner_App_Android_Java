package com.kapstranspvtltd.kaps_partner.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class APIHelper {
    public static void sendOTP(Context context, String mobileNumber,
                               Response.Listener<JSONObject> successListener,
                               Response.ErrorListener errorListener) {
        
        try {
            String url = APIClient.baseUrl + "send_otp";

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("mobile_no", mobileNumber);

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                successListener,
                errorListener
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            VolleySingleton.getInstance(context).addToRequestQueue(request);

        } catch (Exception e) {
            e.printStackTrace();
            errorListener.onErrorResponse(new VolleyError("Error preparing request"));
        }
    }
}