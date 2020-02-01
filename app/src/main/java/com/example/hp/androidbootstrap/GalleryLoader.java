package com.example.hp.androidbootstrap;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.cloudinary.Transformation;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.preprocess.BitmapDecoder;
import com.cloudinary.android.preprocess.BitmapEncoder;
import com.cloudinary.android.preprocess.DimensionsValidator;
import com.cloudinary.android.preprocess.ImagePreprocessChain;
import com.cloudinary.android.preprocess.Limit;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Map;


public class GalleryLoader extends AppCompatActivity {

    static int REQUEST_CODE=7;
    private ImageView imageView;
    private TextView textView;
    private String path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_loader);
        imageView=(ImageView)findViewById(R.id.imageView);
        textView=(TextView)findViewById(R.id.textView);
        Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent,"Select Picture"),REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK)
        {
            if(requestCode==REQUEST_CODE)
            {
                Uri imageUri=data.getData();
                if(imageUri!=null) {
                    path = getPathFromURI(imageUri);
                    imageView.setImageURI(imageUri);
                    uploadToCloud();
                }
            }
        }
    }


    public String getPathFromURI(Uri imageUri)
    {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(imageUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }


    public String searchGoogle(String cloudURL)
    {
        String searchURL = "http://www.google.com/searchbyimage?image_url=" + cloudURL;
        Connection connect= Jsoup.connect(searchURL);
        connect.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21");
        connect.timeout(30000);
        try
        {
            Document document=connect.get();
            Element div=document.getElementsByClass("r5a77d").get(0);
            final String answer=div.getElementsByTag("a").get(0).text();
            return answer;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "nothing";
    }

    public void uploadToCloud()
    {
        Transformation tr=new Transformation();
        tr.crop("fit").width(100).angle(90);
        String requestId = MediaManager.get().upload(path).preprocess(new ImagePreprocessChain()
                .loadWith(new BitmapDecoder(1000, 1000))
                .addStep(new Limit(1000, 1000))
                .addStep(new DimensionsValidator(10,10,1000,1000))
                .saveWith(new BitmapEncoder(BitmapEncoder.Format.WEBP, 80))).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {

            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {

            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String cloudURL=resultData.get("url").toString();
                textView.setText(cloudURL);
                if(cloudURL!=null)
                    new Scraper().execute(cloudURL);
                else{
                    Log.d("MYTAG","URL is null");
                }
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Log.d("MYTAG",error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {

            }
        }).dispatch(getApplicationContext());
        Log.d("MYTAG",requestId);

    }

    private class Scraper extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... strings) {
            Log.d("MYTAG","In async task");
            return searchGoogle(strings[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("MYTAG",s);
            textView.setText(s);
        }
    }


}
