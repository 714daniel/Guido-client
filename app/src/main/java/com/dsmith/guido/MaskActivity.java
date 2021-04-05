package com.dsmith.guido;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class MaskActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mask);
        String filename = UUID.randomUUID().toString() + ".png";
        Log.e("MASKACTIVITY", "RuNNING MAS");
        File file = new File(getFilesDir(), filename);
        Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                BuildConfig.APPLICATION_ID + ".provider", file);
        Log.e("FILESPATH:",uri.getPath());
        Log.e("FILESPATH2:",uri.toString());
        ActivityResultLauncher<Uri> mGetContent = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        sendPictureToAPI(uri, filename);
                    }
                });
        mGetContent.launch(uri);
    }

    protected String sendPictureToAPI(Uri uri, String filename) {
        try {
            Log.e("maskActivity", "REQUESTING RESPONSE");
            RequestQueue queue = Volley.newRequestQueue(this);
            String ip = "http://192.168.0.119:8080";
            String url = ip + "/upload/";
            JSONObject body = new JSONObject();
            final String[] bodyResponse = {""};

            ImageDecoder.Source imageDecoder = ImageDecoder.createSource(this.getContentResolver(), uri);
            ByteArrayOutputStream imageOutput = new ByteArrayOutputStream();
            Bitmap image = ImageDecoder.decodeBitmap(imageDecoder);
            image.compress(Bitmap.CompressFormat.PNG, 100, imageOutput);
            body.put("photo", Base64.getEncoder().encodeToString(imageOutput.toByteArray()));
            final String requestBody = body.toString();
            Intent intent = new Intent(this, DisplayImageActivity.class);

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                bodyResponse[0] = response;
                Bundle b = new Bundle();
                b.putString("UID", bodyResponse[0]);
                b.putString("URI", uri.getPath());
                b.putString("IP", ip);
                intent.putExtras(b);
                startActivity(intent);
                finish();
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                Log.e("maskActivity", "big oops on network");
                Log.e("maskNetworkActivity", error.toString());
            }
        }) {
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return body == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Log.e("maskActivity", "PARSING RESPONSE");
                String responseString = "";
                String responseMessage = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                    try {
                        responseMessage = new String(response.data, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                return Response.success(responseMessage, HttpHeaderParser.parseCacheHeaders(response));
            }
        };
            queue.add(stringRequest);
    } catch (JSONException | IOException f) {
        Log.e("maskActivity", "big oops");
    }
        return null;
    }
}