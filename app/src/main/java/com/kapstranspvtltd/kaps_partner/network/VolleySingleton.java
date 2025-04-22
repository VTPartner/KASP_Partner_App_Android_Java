package com.kapstranspvtltd.kaps_partner.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class VolleySingleton {
    private static volatile VolleySingleton INSTANCE;
    private final RequestQueue requestQueue;

    private VolleySingleton(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static VolleySingleton getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (VolleySingleton.class) {
                if (INSTANCE == null) {
                    INSTANCE = new VolleySingleton(context);
                }
            }
        }
        return INSTANCE;
    }

    public <T> void addToRequestQueue(Request<T> request) {
        requestQueue.add(request);
    }

    // Optional: Method to get RequestQueue directly if needed
    public RequestQueue getRequestQueue() {
        return requestQueue;
    }
}