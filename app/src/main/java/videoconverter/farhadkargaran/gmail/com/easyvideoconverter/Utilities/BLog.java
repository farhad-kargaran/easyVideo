package videoconverter.farhadkargaran.gmail.com.easyvideoconverter.Utilities;


import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.BuildConfig;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.VideoApp;

/**
 * Created by Babax on 1/9/2017.
 */

public class BLog {
    public static void d(String tag, String content) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, content);
        }
    }

    public static void e(String tag, String content) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, content);
        }
    }

    public static void e(String content) {

        if (BuildConfig.DEBUG) {
            android.util.Log.e(VideoApp.TAG, content);
        }
    }
    public static void e(Exception e) {

        if (BuildConfig.DEBUG) {
            android.util.Log.e(VideoApp.TAG, e.getMessage());
        }
    }

    public static void i(String tag, String content) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, content);
        }
    }
}
