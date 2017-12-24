package videoconverter.farhadkargaran.gmail.com.easyvideoconverter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;


import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.Utilities.Helper;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_TAKE_GALLERY_VIDEO = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fabGallary = (FloatingActionButton) findViewById(R.id.fabgallary);
        fabGallary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);

            }
        });


    }

    String selectedVideoPath;
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                Uri selectedVideoUri = data.getData();
                selectedVideoPath = null;
                // MEDIA GALLERY
                selectedVideoPath = Helper.getPath(selectedVideoUri);
                if (selectedVideoPath != null) {

                    Intent intent = new Intent(MainActivity.this, TranscodeAvtivity.class);
                    intent.putExtra("path", selectedVideoPath);
                    startActivity(intent);
                }
            }

        }
    }



}
