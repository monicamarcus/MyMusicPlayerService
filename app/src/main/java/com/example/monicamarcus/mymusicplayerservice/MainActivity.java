package com.example.monicamarcus.mymusicplayerservice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String endpoint = "http://streaming.earbits.com/api/v1/track.json?stream_id=5654d7c3c5aa6e00030021aa";

    private static final String TAG_ARTIST_NAME = "artist_name";
    private static final String TAG_TRACK_NAME = "name";
    private static final String TAG_COVER_IMAGE_URL = "cover_image";
    private static final String TAG_TRACK_URL = "media_file";

    private static ArrayList<Song> songList = new ArrayList<>(0);
    private static Song currentSong = null;
    private static int currentPosition = -1;

    private TextView tvResponse;
    private TextView tvIsConnected;
    private ImageView coverImage;
    private Button nextTrackBtn;
    private Button previousTrackBtn;
    private ProgressDialog mProgressDialog;
    private MediaPlayer mp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResponse = (TextView) findViewById(R.id.tvResponse);
        tvIsConnected = (TextView) findViewById(R.id.tvIsConnected);
        coverImage = (ImageView) findViewById(R.id.image);
        previousTrackBtn = (Button) findViewById(R.id.previousTrackBt);
        previousTrackBtn.setOnClickListener(handlerPreviousTrack);
        nextTrackBtn = (Button) findViewById(R.id.nextTrackBt);
        nextTrackBtn.setOnClickListener(handlerNextTrack);
        // check if you are connected or not
        if (isConnected()) {
            tvIsConnected.setBackgroundColor(0xFF00CC00);
            tvIsConnected.setText("You are connected");
        } else {
            tvIsConnected.setText("You are NOT connected");
        }
    }

    View.OnClickListener handlerNextTrack = new View.OnClickListener() {
        public void onClick(View v) {
            if (mp != null) mp.stop();
            if ((!songList.isEmpty()) && currentPosition < songList.size() - 1) {
                Toast.makeText(getBaseContext(), "Not a new song", Toast.LENGTH_LONG).show();
                currentPosition++;
                currentSong = songList.get(currentPosition);
                tvResponse.setText(currentSong.toString());
                new getImageAsyncTask().execute(currentSong.getImageURL());
                play(currentSong.getTrackURL());
            } else
                new HttpAsyncTask().execute(endpoint);
            if (currentSong != null && !songList.contains(currentSong)) {
                songList.add(currentSong);
                currentPosition = songList.indexOf(currentSong);
            }
        }
    };

    View.OnClickListener handlerPreviousTrack = new View.OnClickListener() {
        public void onClick(View v) {
            if (mp != null) mp.stop();
            songList.trimToSize();
            if (!songList.isEmpty()) {
                currentPosition = songList.indexOf(currentSong) - 1;
                if (currentPosition >= 0) {
                    currentSong = songList.get(currentPosition);
                    tvResponse.setText(currentSong.toString());
                    new getImageAsyncTask().execute(currentSong.getImageURL());
                    play(currentSong.getTrackURL());
                } else {
                    tvResponse.setText("There is no previous song in the list");
                    coverImage.setImageBitmap(null);
                }
            } else {
                currentSong = null;
                currentPosition = -1;
                tvResponse.setText("Press the Next Track button, the list is empty.");
                coverImage.setImageBitmap(null);
            }
        }
    };

    //play the track from external URL
    public void play(String trackURL) {
        try {
            mp = new MediaPlayer();
            mp.setDataSource(trackURL);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //When the Pause button is clicked
    public void pause(View view) {
        if (mp != null)
            mp.pause();
    }

    //When the Resume button is clicked
    public void resume(View view) {
        if (mp != null)
            mp.start();
    }

    //When the Stop button is clicked
    public void stop(View view) {
        if (mp != null) {
            mp.stop();
            mp.release();
            mp = null;
        }
    }

    public static String GET(URL url) {
        String result = "";
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            // convert inputstream to string
            if (is != null)
                result = convertInputStreamToString(is);
            else
                result = "Did not work!";
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null) {
            result += line;
        }
        inputStream.close();
        return result;
    }

    public boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private class getImageAsyncTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setTitle("Download Cover Image");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected Bitmap doInBackground(String... urls) {

            String imageURL = urls[0];

            Bitmap bitmap = null;
            try {
                // Download Image from URL
                InputStream input = new URL(imageURL).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result == null) {
                Toast.makeText(getBaseContext(), "Image not available", Toast.LENGTH_LONG).show();
            }
            // Set the bitmap into ImageView
            coverImage.setImageBitmap(result);
            mProgressDialog.dismiss();
        }
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            URL url = null;
            try {
                url = new URL(urls[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return GET(url);
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "New song!", Toast.LENGTH_LONG).show();
            try {
                JSONObject json = new JSONObject(result);
                currentSong = new Song(getTrackURL(json), getTrackName(json), getArtistName(json), getCoverImageURL(json));
                currentPosition++;
                tvResponse.setText(currentSong.toString());
                String imageURL = getCoverImageURL(json);
                new getImageAsyncTask().execute(imageURL);
                String trackURL = getTrackURL(json);
                currentSong.setImageURL(imageURL);
                songList.add(currentSong);
                play(trackURL);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private String getTrackName(JSONObject json) {
        String trackName = "";
        try {
            trackName = json.getString(TAG_TRACK_NAME);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return trackName;
        }
    }

    private String getArtistName(JSONObject json) {
        String artistName = "";
        try {
            artistName = json.getString(TAG_ARTIST_NAME);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return artistName;
        }
    }

    private String getCoverImageURL(JSONObject json) {
        String imageURL = "";
        try {
            imageURL = json.getString(TAG_COVER_IMAGE_URL);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return imageURL;
        }
    }

    private String getTrackURL(JSONObject json) {
        String trackURL = "";
        try {
            trackURL = json.getString(TAG_TRACK_URL);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return trackURL;
        }
    }
}
