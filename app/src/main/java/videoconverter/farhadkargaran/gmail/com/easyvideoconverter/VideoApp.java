package videoconverter.farhadkargaran.gmail.com.easyvideoconverter;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;

import java.io.File;

/**
 * Created by Farhad on 12/23/2017.
 */

public class VideoApp extends Application {

    public static final String TAG = "com.easyvideoconverter";
    public static volatile Handler applicationHandler;
    public static volatile Context applicationContext;

    public final static String Folder_Media = "Media";
    public final static String Folder_Video = "Video";
    public final static String Folder_Sent = "Sent";
    public static String MediaFolder;

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        applicationHandler = new Handler(getApplicationContext().getMainLooper());
        MediaFolder = Environment.getExternalStorageDirectory().toString() + File.separator + getString(R.string.app_name);

    }
}
