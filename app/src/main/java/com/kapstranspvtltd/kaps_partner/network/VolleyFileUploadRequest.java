package com.kapstranspvtltd.kaps_partner.network;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public abstract class VolleyFileUploadRequest extends Request<NetworkResponse> {
    private final Response.Listener<NetworkResponse> listener;
    private final String boundary;

    public VolleyFileUploadRequest(int method,
                                 String url,
                                 Response.Listener<NetworkResponse> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
        this.boundary = "boundary=" + System.currentTimeMillis();
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        try {
            // Add data parts
            Map<String, DataPart> data = getByteData();
            if (data != null && !data.isEmpty()) {
                for (Map.Entry<String, DataPart> entry : data.entrySet()) {
                    String key = entry.getKey();
                    DataPart dataPart = entry.getValue();

                    dataOutputStream.writeBytes("--" + boundary + "\r\n");
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + 
                        key + "\"; filename=\"" + dataPart.fileName + "\"\r\n");
                    dataOutputStream.writeBytes("Content-Type: " + dataPart.mimeType + "\r\n\r\n");

                    dataOutputStream.write(dataPart.data);
                    dataOutputStream.writeBytes("\r\n");
                }
            }

            // Close multipart form data
            dataOutputStream.writeBytes("--" + boundary + "--\r\n");

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        listener.onResponse(response);
    }

    public abstract Map<String, DataPart> getByteData() throws AuthFailureError;

    public static class DataPart {
        private final String fileName;
        private final byte[] data;
        private final String mimeType;

        public DataPart(String fileName, byte[] data, String mimeType) {
            this.fileName = fileName;
            this.data = data;
            this.mimeType = mimeType;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getData() {
            return data;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}