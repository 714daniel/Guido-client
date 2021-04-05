package com.dsmith.guido;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;

public class DisplayImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Handler handler = new Handler();
        final String[] response = {""};
        Context context = this;
        Thread requestThread = new Thread() {
            public void run() {
                response[0] = makeRequest(handler, this, context);
            }
        };
        requestThread.start();
        Log.e("STOREDIMG", response[0]);
    }

    private String makeRequest(Handler handler, Thread r, Context context) {
        Log.e("displayImage", "RECURSIVE CALL");
        Bundle b = getIntent().getExtras();
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = b.getString("IP") + "/fetch/";
        Log.e("URI IS", b.getString("URI"));
        JSONObject body = new JSONObject();
        final String[] bodyResponse = {""};
        try {
            body.put("uid", b.getString("UID"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.e("displayimage1", "response is " + Base64.getDecoder().decode(response.getBytes("UTF-8")).toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if(response.equals("pending")) {
                    handler.postDelayed(r, 10000);
                } else {
                    Bitmap decodedImg = null;
                    byte[] imgAr = Base64.getDecoder().decode(response);
                    decodedImg = BitmapFactory.decodeByteArray(imgAr, 0, imgAr.length);

                    Uri uri = Uri.parse(b.getString("URI"));

                    String dest = getFilesDir() + "/" + b.getString("URI").split("/")[2];
                    Log.e("DESTINATION", dest);
                    try(FileOutputStream f = new FileOutputStream(dest)) {
                        decodedImg.compress(Bitmap.CompressFormat.PNG, 100, f);
                        Log.e("succ", decodedImg.toString());
                    } catch (IOException e) {
                        Log.e("IOEXCEPT", e.getMessage());
                    }
                    MediaScannerConnection.scanFile(context,
                            new String[]{dest}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.e("filescane", dest);
                                }
                            });
                    //Bundle b = new Bundle();
                    //b.putString("img", response);
                    //Intent intent = new Intent(context, DisplayCloakedImageActivity.class);
                    //intent.putExtras(b);
                    //startActivity(intent);
                    ImageView imageView =  (ImageView) findViewById(R.id.backImage);
                    imageView.setImageBitmap(decodedImg);
                    handler.removeCallbacks(r);
                }

                bodyResponse[0] = response;

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
                    Log.e("BODY: ",  body.toString().getBytes("utf-8").toString());
                    return body == null ? null : body.toString().getBytes("utf-8");
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
        return bodyResponse[0];
    }

}