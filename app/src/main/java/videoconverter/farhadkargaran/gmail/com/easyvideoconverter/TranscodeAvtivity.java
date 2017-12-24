package videoconverter.farhadkargaran.gmail.com.easyvideoconverter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.util.Matrix;
import com.googlecode.mp4parser.util.Path;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.Utilities.BLog;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.Utilities.Helper;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.videoTranscode.InputSurface;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.videoTranscode.LayoutHelper;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.videoTranscode.MP4Builder;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.videoTranscode.Mp4Movie;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.videoTranscode.OutputSurface;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.videoTranscode.VideoObject;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.videoTranscode.VideoSeekBarView;
import videoconverter.farhadkargaran.gmail.com.easyvideoconverter.videoTranscode.VideoTimelineView;

import static videoconverter.farhadkargaran.gmail.com.easyvideoconverter.Utilities.Helper.displayMetrics;


public class TranscodeAvtivity extends Activity implements TextureView.SurfaceTextureListener {

    private QualityChooseView qualityChooseView;
    private int selectedCompression;
    private int compressionsCount = -1;
    private int compressItem = 0;

    boolean noCompress = true;
    boolean noTrim = true, facedErrorWhileConverting = false;
    public String outputFile;
    public ProgressBar progressBar;
    private TextureView textureView = null;
    public final String MIME_TYPE = "video/avc";
    private ImageView playButton = null;
    private MediaPlayer videoPlayer = null;
    private VideoTimelineView videoTimelineView = null;
    private VideoSeekBarView videoSeekBarView = null;
    private EditText ed_caption;
    private FrameLayout frame_progress;
    public int actionbarSize = 0;
    private boolean playerPrepared = false;
    //public Uri sourceVideoUri;
    private boolean compress = true;
    public String sourceVideoPath = "";
    public String dstMediaPath = null;
    private String output;
    private TextView title;

    private void handleVideo(Intent intent)
    {
        Parcelable parcelable = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        String type = intent.getType();
        if (parcelable != null) {
            String path;
            if (!(parcelable instanceof Uri)) {
                parcelable = Uri.parse(parcelable.toString());
            }
            Uri uri = (Uri) parcelable;
            path = Helper.getPath(uri);
            if (path != null) {
                if (path.startsWith("file:")) {
                    path = path.replace("file://", "");
                }
                if (type != null && type.startsWith("video/")) {
                    sourceVideoPath = path;
                }
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transcode);



        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {


            if (type.startsWith("video/")) {
                handleVideo(intent);
            }
        }


        if(TextUtils.isEmpty(sourceVideoPath))
            sourceVideoPath = getIntent().getExtras().getString("path");//getRealPathFromURI(Uri.parse(getIntent().getExtras().getString("uri")));
        density = getResources().getDisplayMetrics().density;

        try {
            String mediaFilename = new File(sourceVideoPath).getName();
            Long tsLong = System.currentTimeMillis() / 1000;
            dstMediaPath = genDstPath(mediaFilename, tsLong.toString());
        } catch (Exception ex) {
        }


        boolean res = !processOpenVideo();
        if (sourceVideoPath == null) {

            Toast.makeText(TranscodeAvtivity.this,getString(R.string.invalid_video), Toast.LENGTH_LONG ).show();
            finishTranscoding(true);
            return;
        }


        init();


        if (Build.VERSION.SDK_INT < 18) {
            try {
                MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
                if (codecInfo == null) {
                    setGone();
                } else {
                    String name = codecInfo.getName();
                    if (name.equals("OMX.google.h264.encoder") ||
                            name.equals("OMX.ST.VFM.H264Enc") ||
                            name.equals("OMX.Exynos.avc.enc") ||
                            name.equals("OMX.MARVELL.VIDEO.HW.CODA7542ENCODER") ||
                            name.equals("OMX.MARVELL.VIDEO.H264ENCODER") ||
                            name.equals("OMX.k3.video.encoder.avc") || //fix this later
                            name.equals("OMX.TI.DUCATI1.VIDEO.H264E")) { //fix this later
                        setGone();
                    } else {
                        if (selectColorFormat(codecInfo, MIME_TYPE) == 0) {
                            setGone();
                        }
                    }
                }
            } catch (Exception e) {
                setGone();
                //FileBLog.e("tmessages", e);
            }
        }

        updateWidthHeightBitrateForCompression();
        updateVideoEditedInfo();


    }
    private void finishTranscoding(boolean hasError) {
        converting = false;
        try
        {
            if (hasError) output = Uri.parse(Uri.decode(sourceVideoPath)).toString();
            else output = Uri.parse(Uri.decode(dstMediaPath)).toString();//NullPointerException: uriString
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        String content1 = getString(R.string.video_editing_done1) +" "+ getString(R.string.app_name) + " "  +  getString(R.string.video_editing_done2);

        if(!hasError) {
            new MaterialDialog.Builder(TranscodeAvtivity.this)

                    .content(content1)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.shareit)
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent shareIntent = new Intent(
                                    android.content.Intent.ACTION_SEND);
                            shareIntent.setType("video/*");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(dstMediaPath));
                            startActivity(Intent.createChooser(shareIntent,
                                    getString(R.string.shareit)));
                        }
                    })
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .cancelable(false)
                    .show();
        }
        else
        {
            new MaterialDialog.Builder(TranscodeAvtivity.this)

                    .content(R.string.video_editing_failed)
                    .positiveText(R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .cancelable(false)
                    .show();
        }

    }
    public static float getPixelsInCM(float cm, boolean isX) {
        return (cm / 2.54f) * (isX ? displayMetrics.xdpi : displayMetrics.ydpi);
    }

    private void updateWidthHeightBitrateForCompression() {
        if (compressionsCount == -1) {
            if (originalWidth > 1280 || originalHeight > 1280) {
                compressionsCount = 5;
            } else if (originalWidth > 848 || originalHeight > 848) {
                compressionsCount = 4;
            } else if (originalWidth > 640 || originalHeight > 640) {
                compressionsCount = 3;
            } else if (originalWidth > 480 || originalHeight > 480) {
                compressionsCount = 2;
            } else {
                compressionsCount = 1;
            }
            if (compressionsCount > 2)//
                selectedCompression = 2;
        }
        if (selectedCompression >= compressionsCount) {
            selectedCompression = compressionsCount - 1;
        }
        if (selectedCompression != compressionsCount - 1) {
            float maxSize;
            int targetBitrate;
            switch (selectedCompression) {
                case 0:
                    maxSize = 432.0f;
                    targetBitrate = 400000;
                    break;
                case 1:
                    maxSize = 640.0f;
                    targetBitrate = 900000;
                    break;
                case 2:
                    maxSize = 848.0f;
                    targetBitrate = 1100000;
                    break;
                case 3:
                default:
                    targetBitrate = 1600000;
                    maxSize = 1280.0f;
                    break;
            }
            float scale = originalWidth > originalHeight ? maxSize / originalWidth : maxSize / originalHeight;
            resultWidth = Math.round(originalWidth * scale / 2) * 2;
            resultHeight = Math.round(originalHeight * scale / 2) * 2;
            if (bitrate != 0) {
                bitrate = Math.min(targetBitrate, (int) (originalBitrate / scale));
                videoFramesSize = (long) (bitrate / 8 * videoDuration / 1000);


            }
        }
    }

    private class QualityChooseView extends View {

        private Paint paint;
        private TextPaint textPaint;

        private int circleSize;
        private int gapSize;
        private int sideSide;
        private int lineSize;

        private boolean moving;
        private boolean startMoving;
        private float startX;

        private int startMovingQuality;

        public QualityChooseView(Context context) {
            super(context);

            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(dp(12));
            textPaint.setColor(0xffcdcdcd);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
                for (int a = 0; a < compressionsCount; a++) {
                    int cx = sideSide + (lineSize + gapSize * 2 + circleSize) * a + circleSize / 2;
                    if (x > cx - dp(15) && x < cx + dp(15)) {
                        startMoving = a == selectedCompression;
                        startX = x;
                        startMovingQuality = selectedCompression;
                        break;
                    }
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (startMoving) {
                    if (Math.abs(startX - x) >= getPixelsInCM(0.5f, true)) {
                        moving = true;
                        startMoving = false;
                    }
                } else if (moving) {
                    for (int a = 0; a < compressionsCount; a++) {
                        int cx = sideSide + (lineSize + gapSize * 2 + circleSize) * a + circleSize / 2;
                        int diff = lineSize / 2 + circleSize / 2 + gapSize;
                        if (x > cx - diff && x < cx + diff) {
                            if (selectedCompression != a) {
                                selectedCompression = a;
                                didChangedCompressionLevel(false);
                                invalidate();
                            }
                            break;
                        }
                    }
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (!moving) {
                    for (int a = 0; a < compressionsCount; a++) {
                        int cx = sideSide + (lineSize + gapSize * 2 + circleSize) * a + circleSize / 2;
                        if (x > cx - dp(15) && x < cx + dp(15)) {
                            if (selectedCompression != a) {
                                selectedCompression = a;
                                didChangedCompressionLevel(true);
                                invalidate();
                            }
                            break;
                        }
                    }
                } else {
                    if (selectedCompression != startMovingQuality) {
                        updateWidthHeightBitrateForCompression();
                        updateVideoEditedInfo();
                    }
                }
                startMoving = false;
                moving = false;
            }
            return true;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            circleSize = dp(16);
            gapSize = dp(3);
            sideSide = dp(18);
            lineSize = (getMeasuredWidth() - circleSize * compressionsCount - gapSize * 8 - sideSide * 2) / (compressionsCount - 1);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int cy = getMeasuredHeight() / 2 + dp(6);
            for (int a = 0; a < compressionsCount; a++) {
                int cx = sideSide + (lineSize + gapSize * 2 + circleSize) * a + circleSize / 2;
                if (a <= selectedCompression) {
                    paint.setColor(0xff53aeef);
                } else {
                    //paint.setColor(bright_primary);
                    paint.setColor(0xff222222);
                }
                String text;
                if (a == compressionsCount - 1) {
                    text = originalHeight + "p";
                } else if (a == 0) {
                    text = "240p";
                } else if (a == 1) {
                    text = "360p";
                } else if (a == 2) {
                    text = "480p";
                } else {
                    text = "720p";
                }

                float width = textPaint.measureText(text);

                int aa = paint.getColor();
                if (a <= selectedCompression) paint.setColor(getResources().getColor(R.color.colorPrimaryLight));
                canvas.drawCircle(cx, cy, a == selectedCompression ? dp(8) : circleSize / 2, paint);
                paint.setColor(aa);
                canvas.drawText(text, cx - width / 2, cy - dp(16), textPaint);
                if (a != 0) {
                    int x = cx - circleSize / 2 - gapSize - lineSize;
                    aa = paint.getColor();
                    if (a <= selectedCompression) paint.setColor(getResources().getColor(R.color.colorPrimaryLight));
                    canvas.drawRect(x, cy - dp(1), x + lineSize, cy + dp(2), paint);
                    paint.setColor(aa);
                }
            }
        }
    }

    private void didChangedCompressionLevel(boolean request) {
        SharedPreferences preferences = VideoApp.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("compress_video2", selectedCompression);
        editor.commit();
        updateWidthHeightBitrateForCompression();
        updateVideoEditedInfo();
        /*if (request) {
            requestVideoPreview(1);
        }*/
    }

    private void setGone() {
    }

    @Override
    public void onResume() {
        super.onResume();

        fixLayoutInternal();
    }

    public String genDstPath(String srcName, String effect) {
        /*int indx = srcName.lastIndexOf('.');
        if(indx<=0 && ex.length()>1) srcName = srcName + ex;*/
        String substring = srcName.substring(0, srcName.lastIndexOf('.'));
        File outputFolder;

//        if (GifChecker.isChecked()) {
//            outputFolder = new File(SmsApp.MediaFolder + File.separator + AppConstant.Folder_Gif + File.separator + AppConstant.Folder_Sent);
//
//        } else {

        outputFolder = new File(VideoApp.MediaFolder);
//        }


        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        return (outputFolder.getPath() + File.separator + substring + "_" + effect + ".mp4");
    }

    private View videoContainerView = null;
    private View controlView = null;
    private View textContainerView = null;

    private void fixLayoutInternal() {

        if (videoContainerView == null) {
            finishTranscoding(true);
            return;
        }

        if (!Helper.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) videoContainerView.getLayoutParams();
            layoutParams.topMargin = Helper.dp(16);
            layoutParams.bottomMargin = Helper.dp(16);
            layoutParams.width = Helper.displaySize.x / 3 - Helper.dp(24);
            layoutParams.leftMargin = Helper.dp(16);
            videoContainerView.setLayoutParams(layoutParams);

            layoutParams = (FrameLayout.LayoutParams) controlView.getLayoutParams();
            layoutParams.topMargin = Helper.dp(16);
            layoutParams.bottomMargin = 0;
            layoutParams.width = Helper.displaySize.x / 3 * 2 - Helper.dp(32);
            layoutParams.leftMargin = Helper.displaySize.x / 3 + Helper.dp(16);
            layoutParams.gravity = Gravity.TOP;
            controlView.setLayoutParams(layoutParams);

            layoutParams = (FrameLayout.LayoutParams) textContainerView.getLayoutParams();
            layoutParams.width = Helper.displaySize.x / 3 * 2 - Helper.dp(32);
            layoutParams.leftMargin = Helper.displaySize.x / 3 + Helper.dp(16);
            layoutParams.rightMargin = Helper.dp(16);
            layoutParams.bottomMargin = Helper.dp(16);
            textContainerView.setLayoutParams(layoutParams);
        } else {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) videoContainerView.getLayoutParams();
            //layoutParams.topMargin = Helper.dp(16);
            layoutParams.topMargin = Helper.dp(Helper.convertPixelsToDp(actionbarSize + 30));
            layoutParams.bottomMargin = Helper.dp(260);
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.leftMargin = 0;
            videoContainerView.setLayoutParams(layoutParams);

            layoutParams = (FrameLayout.LayoutParams) controlView.getLayoutParams();
            layoutParams.topMargin = 0;
            layoutParams.leftMargin = 0;
            layoutParams.bottomMargin = Helper.dp(150);
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.gravity = Gravity.BOTTOM;
            controlView.setLayoutParams(layoutParams);

            layoutParams = (FrameLayout.LayoutParams) textContainerView.getLayoutParams();
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.leftMargin = Helper.dp(16);
            layoutParams.rightMargin = Helper.dp(16);
            layoutParams.bottomMargin = Helper.dp(16);
            //layoutParams.topMargin = Helper.dp(16);
            textContainerView.setLayoutParams(layoutParams);
        }
        fixVideoSize();
        videoTimelineView.clearFrames();
    }

    private void fixVideoSize() {

        int viewHeight;
        if (Helper.isTablet()) {
            viewHeight = Helper.dp(472);
        } else {
            viewHeight = Helper.displaySize.y - Helper.getStatusBarHeight();// - AndroidUtilities.actionbarSize;

        }
        BLog.d("fffggg", "AndroidUtilities.displaySize.y = " + Helper.displaySize.y);
        int width;
        int height;
        if (Helper.isTablet()) {
            width = Helper.dp(490);
            height = viewHeight - Helper.dp(276);
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = Helper.displaySize.x / 3 - Helper.dp(24);
                height = viewHeight - Helper.dp(32);
            } else {
                width = Helper.displaySize.x;
                height = viewHeight - Helper.dp(276);
            }
        }


        int vwidth = rotationValue == 90 || rotationValue == 270 ? originalHeight : originalWidth;
        int vheight = rotationValue == 90 || rotationValue == 270 ? originalWidth : originalHeight;
        float wr = (float) width / (float) vwidth;
        float hr = (float) height / (float) vheight;
        float ar = (float) vwidth / (float) vheight;

        if (wr > hr) {
            width = (int) (height * ar);
        } else {
            height = (int) (width / ar);
        }

        if (textureView != null) {
            //FrameLayout.LayoutParams layoutParamsFrameProgress = (FrameLayout.LayoutParams) frame_progress.getLayoutParams();
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textureView.getLayoutParams();
            FrameLayout.LayoutParams layoutParamsP = (FrameLayout.LayoutParams) progressBar.getLayoutParams();

           /* layoutParamsFrameProgress.width = width;
            layoutParamsFrameProgress.height = height;
            layoutParamsFrameProgress.leftMargin = 0;
            layoutParamsFrameProgress.topMargin = 0;*/

            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.leftMargin = 0;
            layoutParams.topMargin = 30;


            layoutParamsP.width = width;
            layoutParamsP.height = convertDpToPx(10);
            layoutParamsP.leftMargin = 0;
            layoutParamsP.topMargin = 0;
            frame_progress.setLayoutParams(layoutParams);
            textureView.setLayoutParams(layoutParams);
            progressBar.setLayoutParams(layoutParamsP);
        }
    }

    public void back(View v) {
        onBackPressed();
    }

    public static float density = 1;

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    private int convertDpToPx(int dp) {
        return Math.round(dp * (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));

    }

    @Override
    protected void onDestroy() {

        if (videoTimelineView != null) {
            videoTimelineView.destroy();
        }
        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
                videoPlayer.release();
                videoPlayer = null;
            } catch (Exception e) {
                //FileBLog.e("tmessages", e);
            }
        }

        super.onDestroy();
    }

    public void cancelVideoConvert(VideoObject videoObject) {
        if (videoObject == null) {
            synchronized (videoConvertSync) {
                BLog.e("ffaa", "canceld = true");
                cancelCurrentVideoConversion = true;
            }
        }
    }

    private void updateVideoOriginalInfo() {

        int width = rotationValue == 90 || rotationValue == 270 ? originalHeight : originalWidth;
        int height = rotationValue == 90 || rotationValue == 270 ? originalWidth : originalHeight;
        String videoDimension = String.format("%dx%d", width, height);
        long duration = (long) Math.ceil(videoDuration);
        int minutes = (int) (duration / 1000 / 60);
        int seconds = (int) Math.ceil(duration / 1000) - minutes * 60;
        String videoTimeSize = String.format("%d:%02d, %s", minutes, seconds, Helper.formatFileSize(originalSize));
    }

    int tenPrecentage;
    int goalPrecentage;
    boolean converting = false;

    public void startIt(View v) {
        if (converting) {
            ImageView ivStartStop = (ImageView) findViewById(R.id.ivStartStop);
            //ivStartStop.setImageBitmap(null);
            ivStartStop.setBackgroundDrawable(null);
            ivStartStop.setBackgroundDrawable(Helper.getDrawable(R.drawable.check));
            converting = false;
            cancelVideoConvert(null);
            title.setText(getResources().getString(R.string.editvideo));
            progressBar.setProgress(0);
            finish();
            return;
        }

        if (noNeedToTransfer) {
            finishTranscoding(false);
        }
        tenPrecentage = estimatedSize / 10;
        goalPrecentage = estimatedSize - tenPrecentage;
        //progressBar.setMax(estimatedSize);
        //progressBar.setVisibility(View.VISIBLE);
        //progressBar.setProgress(0);
        startVideoConvert();
    }

    @Override
    public void onBackPressed() {
        cancelVideoConvert(null);
        super.onBackPressed();
    }

    private void setupVideoplayer() {
        videoPlayer = new MediaPlayer();
        videoPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Helper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        onPlayComplete();
                    }
                });
            }
        });
        videoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                playerPrepared = true;
                if (videoTimelineView != null && videoPlayer != null) {
                    videoPlayer.seekTo((int) (videoTimelineView.getLeftProgress() * videoDuration));
                }
            }
        });
        try {
            videoPlayer.setDataSource(sourceVideoPath);
            videoPlayer.prepareAsync();
        } catch (Exception e) {
            //FileBLog.e("tmessages", e);
            return;
        }
    }

    private void init() {

        /*int resourceId = getResources().getId("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            Helper.statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }*/


        if (compressionsCount > 1) {
            compressItem = 1;
            LinearLayout ln_quality = (LinearLayout) findViewById(R.id.ln_quality);
            qualityChooseView = new QualityChooseView(TranscodeAvtivity.this);
            //qualityChooseView.setTranslationY(dp(120));
            qualityChooseView.setVisibility(View.VISIBLE);
            //qualityChooseView.setLayoutParams(new ViewGroup.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ln_quality.addView(qualityChooseView);
        }

        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionbarSize = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            BLog.d("fffggg", "2 actionbarSize = " + actionbarSize);
        }

        //region gif


        //endregion

        //mVideoView = (VideoView) findViewById(R.id.mvideoview);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        title = (TextView) findViewById(R.id.tv2);
        ed_caption = (EditText) findViewById(R.id.ed_caption);
        videoContainerView = findViewById(R.id.vvideo_container);
        controlView = findViewById(R.id.control_layout);
        textContainerView = findViewById(R.id.info_container);

        if (Build.VERSION.SDK_INT < 18) {
            try {
                MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
                if (codecInfo == null) {
                    setGone();
                } else {
                    String name = codecInfo.getName();
                    if (name.equals("OMX.google.h264.encoder") ||
                            name.equals("OMX.ST.VFM.H264Enc") ||
                            name.equals("OMX.Exynos.avc.enc") ||
                            name.equals("OMX.MARVELL.VIDEO.HW.CODA7542ENCODER") ||
                            name.equals("OMX.MARVELL.VIDEO.H264ENCODER") ||
                            name.equals("OMX.k3.video.encoder.avc") || //fix this later
                            name.equals("OMX.TI.DUCATI1.VIDEO.H264E")) { //fix this later
                        setGone();
                    } else {
                        if (selectColorFormat(codecInfo, MIME_TYPE) == 0) {
                            setGone();
                        }
                    }
                }
            } catch (Exception e) {
                setGone();
            }
        }


        compress = true;
        //compress = compressVideo.isChecked();


        playButton = (ImageView) findViewById(R.id.pplay_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        setupVideoplayer();

        videoTimelineView = (VideoTimelineView) findViewById(R.id.video_timeline_view);
        videoTimelineView.setVideoPath(sourceVideoPath);
        videoTimelineView.setColor(getResources().getColor(R.color.colorPrimary));
        videoTimelineView.setDelegate(new VideoTimelineView.VideoTimelineViewDelegate() {
            @Override
            public void onLeftProgressChanged(float progress) {
                if (videoPlayer == null || !playerPrepared) {
                    return;
                }
                try {
                    if (videoPlayer.isPlaying()) {
                        videoPlayer.pause();
                        playButton.setImageResource(R.drawable.video_play);
                    }
                    videoPlayer.setOnSeekCompleteListener(null);
                    videoPlayer.seekTo((int) (videoDuration * progress));
                    noTrim = false;
                } catch (Exception e) {
                    int x = 00;
                }
                needSeek = true;
                videoSeekBarView.setProgress(videoTimelineView.getLeftProgress());
                updateVideoEditedInfo();
            }

            @Override
            public void onRifhtProgressChanged(float progress) {
                if (videoPlayer == null || !playerPrepared) {
                    return;
                }
                try {
                    if (videoPlayer.isPlaying()) {
                        videoPlayer.pause();
                        playButton.setImageResource(R.drawable.video_play);
                    }
                    videoPlayer.setOnSeekCompleteListener(null);
                    videoPlayer.seekTo((int) (videoDuration * progress));
                    noTrim = false;
                } catch (Exception e) {
                    //FileBLog.e("tmessages", e);
                }
                needSeek = true;
                videoSeekBarView.setProgress(videoTimelineView.getLeftProgress());
                updateVideoEditedInfo();
            }
        });

        videoSeekBarView = (VideoSeekBarView) findViewById(R.id.video_seekbar);
        videoSeekBarView.color = getResources().getColor(R.color.colorPrimary);
        videoSeekBarView.invalidate();
        videoSeekBarView.delegate = new VideoSeekBarView.SeekBarDelegate() {
            @Override
            public void onSeekBarDrag(float progress) {
                if (progress < videoTimelineView.getLeftProgress()) {
                    progress = videoTimelineView.getLeftProgress();
                    videoSeekBarView.setProgress(progress);
                } else if (progress > videoTimelineView.getRightProgress()) {
                    progress = videoTimelineView.getRightProgress();
                    videoSeekBarView.setProgress(progress);
                }
                if (videoPlayer == null || !playerPrepared) {
                    return;
                }
                if (videoPlayer.isPlaying()) {
                    try {
                        videoPlayer.seekTo((int) (videoDuration * progress));
                        lastProgress = progress;
                    } catch (Exception e) {
                        //FileBLog.e("tmessages", e);
                    }
                } else {
                    lastProgress = progress;
                    needSeek = true;
                }
            }
        };
        textureView = (TextureView) findViewById(R.id.vvideo_view);
        textureView.setSurfaceTextureListener(this);
        frame_progress = (FrameLayout) findViewById(R.id.fframe_progress);
        updateVideoOriginalInfo();
        updateVideoEditedInfo();
    }

    private float lastProgress = 0;
    private boolean needSeek = false;

    private final Object sync = new Object();
    private Thread thread = null;

    private void play() {
        if (videoPlayer == null || !playerPrepared) {
            return;
        }
        if (videoPlayer.isPlaying()) {
            videoPlayer.pause();
            playButton.setImageResource(R.drawable.video_play);
        } else {
            try {
                playButton.setImageDrawable(null);
                lastProgress = 0;
                if (needSeek) {
                    videoPlayer.seekTo((int) (videoDuration * videoSeekBarView.getProgress()));
                    needSeek = false;
                }
                videoPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        float startTime = videoTimelineView.getLeftProgress() * videoDuration;
                        float endTime = videoTimelineView.getRightProgress() * videoDuration;
                        if (startTime == endTime) {
                            startTime = endTime - 0.01f;
                        }
                        lastProgress = (videoPlayer.getCurrentPosition() - startTime) / (endTime - startTime);
                        float lrdiff = videoTimelineView.getRightProgress() - videoTimelineView.getLeftProgress();
                        lastProgress = videoTimelineView.getLeftProgress() + lrdiff * lastProgress;
                        videoSeekBarView.setProgress(lastProgress);
                    }
                });
                videoPlayer.start();
                synchronized (sync) {
                    if (thread == null) {
                        thread = new Thread(progressRunnable);
                        thread.start();
                    }
                }
            } catch (Exception e) {
                //FileBLog.e("tmessages", e);
            }
        }
    }


    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            boolean playerCheck;

            while (true) {
                synchronized (sync) {
                    try {
                        playerCheck = videoPlayer != null && videoPlayer.isPlaying();
                    } catch (Exception e) {
                        playerCheck = false;
                        //FileBLog.e("tmessages", e);
                    }
                }
                if (!playerCheck) {
                    break;
                }
                Helper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (videoPlayer != null && videoPlayer.isPlaying()) {
                            float startTime = videoTimelineView.getLeftProgress() * videoDuration;
                            float endTime = videoTimelineView.getRightProgress() * videoDuration;
                            if (startTime == endTime) {
                                startTime = endTime - 0.01f;
                            }
                            float progress = (videoPlayer.getCurrentPosition() - startTime) / (endTime - startTime);
                            float lrdiff = videoTimelineView.getRightProgress() - videoTimelineView.getLeftProgress();
                            progress = videoTimelineView.getLeftProgress() + lrdiff * progress;
                            if (progress > lastProgress) {
                                videoSeekBarView.setProgress(progress);
                                lastProgress = progress;
                            }
                            if (videoPlayer.getCurrentPosition() >= endTime) {
                                try {
                                    videoPlayer.pause();
                                    onPlayComplete();
                                } catch (Exception e) {
                                    //FileBLog.e("tmessages", e);
                                }
                            }
                        }
                    }
                });
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                    //FileBLog.e("tmessages", e);
                }
            }
            synchronized (sync) {
                thread = null;
            }
        }
    };

    private void onPlayComplete() {
        if (playButton != null) {
            playButton.setImageResource(R.drawable.video_play);
        }
        if (videoSeekBarView != null && videoTimelineView != null) {
            videoSeekBarView.setProgress(videoTimelineView.getLeftProgress());
        }
        try {
            if (videoPlayer != null) {
                if (videoTimelineView != null) {
                    videoPlayer.seekTo((int) (videoTimelineView.getLeftProgress() * videoDuration));
                }
            }
        } catch (Exception e) {
            //FileBLog.e("tmessages", e);
        }
    }

    /*private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }*/

    private final Object videoConvertSync = new Object();
    private ArrayList<VideoObject> videoConvertQueue = new ArrayList<>();
    public boolean cancelCurrentVideoConversion = false;


    private void setPlayerSurface() {
        if (textureView == null || !textureView.isAvailable() || videoPlayer == null) {
            return;
        }
        try {
            Surface s = new Surface(textureView.getSurfaceTexture());
            videoPlayer.setSurface(s);
            if (playerPrepared) {
                videoPlayer.seekTo((int) (videoTimelineView.getLeftProgress() * videoDuration));
            }
        } catch (Exception e) {
            // FileBLog.e("tmessages", e);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        setPlayerSurface();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (videoPlayer == null) {
            return true;
        }
        videoPlayer.setDisplay(null);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }


    /* private  static  class VideoConvertRunnable implements Runnable {

         private VideoObject videoObject;

         private VideoConvertRunnable(VideoObject message) {
             videoObject = message;
         }

         @Override
         public void run() {
             TranscodeAvtivity.getInstance().convertVideo(videoObject);
         }

         public  static  void runConversion(final VideoObject obj) {
             new Thread(new Runnable() {
                 @Override
                 public void run() {
                     try {
                         VideoConvertRunnable wrapper = new VideoConvertRunnable(obj);
                         Thread th = new Thread(wrapper, "VideoConvertRunnable");
                         th.start();
                         th.join();
                     } catch (Exception e) {
                         //FileBLog.e("tmessages", e);
                     }
                 }
             }).start();
         }
     }*/
    private class VideoConvertRunnable implements Runnable {

        private VideoObject videoObject;

        private VideoConvertRunnable(VideoObject message) {
            videoObject = message;
        }

        @Override
        public void run() {
            convertVideo(videoObject);
        }

        public void runConversion(final VideoObject obj) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoConvertRunnable wrapper = new VideoConvertRunnable(obj);
                        Thread th = new Thread(wrapper, "VideoConvertRunnable");
                        th.start();
                        th.join();
                    } catch (Exception e) {
                        //FileBLog.e("tmessages", e);
                    }
                }
            }).start();
        }
    }

    private void startVideoConvert() {

        synchronized (videoConvertSync) {
            cancelCurrentVideoConversion = false;
        }
        ImageView ivStartStop = (ImageView) findViewById(R.id.ivStartStop);
        //ivStartStop.setImageBitmap(null);
        ivStartStop.setBackgroundDrawable(null);
        ivStartStop.setBackgroundDrawable(Helper.getDrawable(R.drawable.canceltranscode));
        converting = true;

        synchronized (sync) {
            if (videoPlayer != null) {
                try {
                    videoPlayer.stop();
                    videoPlayer.release();
                    videoPlayer = null;
                } catch (Exception e) {
                    // FileBLog.e("tmessages", e);
                }
            }
        }

        //videoPlayer.pause();
        //onPlayComplete();

        //vMessageObject vmvessageObject = videoConvertQueue.get(0);
        VideoObject videoObject = new VideoObject();
        videoObject.videoPath = sourceVideoPath;
        videoObject.videoDuration = videoDuration;
        videoObject.startTime = startTime;
        videoObject.rotationValue = rotationValue;
        videoObject.endTime = endTime;
        videoObject.compress = compress;
        videoObject.estimatedSize = estimatedSize;
        BLog.d("ffaa", "compress value is " + compress);
        if (!compress) {
            videoObject.bitrate = originalBitrate;
            videoObject.originalHeight = originalHeight;
            videoObject.originalWidth = originalWidth;
            videoObject.resultHeight = originalHeight;
            videoObject.resultWidth = originalWidth;
        } else {
            videoObject.bitrate = bitrate;
            videoObject.originalHeight = originalHeight;
            videoObject.originalWidth = originalWidth;
            videoObject.resultHeight = resultHeight;
            videoObject.resultWidth = resultWidth;
        }

         /* Intent intent = new Intent(ApplicationLoader.applicationContext, VideoEncodingService.class);
          intent.putExtra("path", vmessageObject.messageOwner.attachPath);
          ApplicationLoader.applicationContext.startService(intent);*/
        //VideoConvertRunnable.runConversion(videoObject);


        //MediaConverter.getInstance().scheduleVideoConvert(videoObject);
        //VideoConvertRunnable.runConversion(videoObject);
        VideoConvertRunnable v = new VideoConvertRunnable(videoObject);
        v.runConversion(videoObject);
    }


    private void didWriteData(final VideoObject videoObject, final File file, final boolean last, final boolean error) {
        final boolean firstWrite = videoConvertFirstWrite;
        if (firstWrite) {
            videoConvertFirstWrite = false;
        }

        Helper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (error) {
                } else {
                    if (firstWrite) {
                    }
                    int fl = (int) (file.length() / 1000);
                    int es = (int) (videoObject.estimatedSize / 1000);
                    //es = 0;

                    if (es == 0) {

                        finishTranscoding(true);
                    }
                    else
                    {
                        int pr = (fl * 100) / es;
                        if (pr > 100) pr = 100;

                        title.setText("" + pr + " %");
                    }

                    //BLog.d("ffaa", "pr = " + pr);
                    //BLog.d("ffaa", " file.length = " + file.length() + " estimated = " + videoObject.estimatedSize);
                }
                if (error || last) {
                    synchronized (videoConvertSync) {
                        cancelCurrentVideoConversion = false;
                    }
                    BLog.d("ffaa", "did did");
                    title.setText(getResources().getString(R.string.editvideo));

                    if (converting)
                        finishTranscoding(false);
                }
            }
        });
    }



    private void checkConversionCanceled() throws Exception {
        boolean cancelConversion;
        synchronized (videoConvertSync) {
            cancelConversion = cancelCurrentVideoConversion;
        }
        if (cancelConversion) {
            BLog.e("ffaa", "canceldeddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");

            throw new RuntimeException("canceled conversion");
        }
    }

    @SuppressLint("NewApi")
    public static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo lastCodecInfo = null;
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    lastCodecInfo = codecInfo;
                    if (!lastCodecInfo.getName().equals("OMX.SEC.avc.enc")) {
                        return lastCodecInfo;
                    } else if (lastCodecInfo.getName().equals("OMX.SEC.AVC.Encoder")) {
                        return lastCodecInfo;
                    }
                }
            }
        }
        return lastCodecInfo;
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    @SuppressLint("NewApi")
    public static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        int lastColorFormat = 0;
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                lastColorFormat = colorFormat;
                if (!(codecInfo.getName().equals("OMX.SEC.AVC.Encoder") && colorFormat == 19)) {
                    return colorFormat;
                }
            }
        }
        return lastColorFormat;
    }

    private boolean videoConvertFirstWrite = true;
    private final static int PROCESSOR_TYPE_OTHER = 0;
    private final static int PROCESSOR_TYPE_QCOM = 1;
    private final static int PROCESSOR_TYPE_INTEL = 2;
    private final static int PROCESSOR_TYPE_MTK = 3;
    private final static int PROCESSOR_TYPE_SEC = 4;
    private final static int PROCESSOR_TYPE_TI = 5;

    @TargetApi(16)
    private boolean convertVideo(final VideoObject videoObject) {


        String videoPath = videoObject.videoPath;
        long startTime = videoObject.startTime;
        long endTime = videoObject.endTime;
        int resultWidth = videoObject.resultWidth;
        int resultHeight = videoObject.resultHeight;
        int rotationValue = videoObject.rotationValue;
        int originalWidth = videoObject.originalWidth;
        int originalHeight = videoObject.originalHeight;
        int bitrate = videoObject.bitrate;

        int rotateRender = 0;
        File cacheFile;
        cacheFile = new File(dstMediaPath);
        if (Build.VERSION.SDK_INT < 18 && resultHeight > resultWidth && resultWidth != originalWidth && resultHeight != originalHeight) {
            int temp = resultHeight;
            resultHeight = resultWidth;
            resultWidth = temp;
            rotationValue = 90;
            rotateRender = 270;
        } else if (Build.VERSION.SDK_INT > 20) {
            if (rotationValue == 90) {
                int temp = resultHeight;
                resultHeight = resultWidth;
                resultWidth = temp;
                rotationValue = 0;
                rotateRender = 270;
            } else if (rotationValue == 180) {
                rotateRender = 180;
                rotationValue = 0;
            } else if (rotationValue == 270) {
                int temp = resultHeight;
                resultHeight = resultWidth;
                resultWidth = temp;
                rotationValue = 0;
                rotateRender = 90;
            }
        }
        outputFile = cacheFile.getAbsolutePath();


        SharedPreferences preferences = VideoApp.applicationContext.getSharedPreferences("videoconvert", Activity.MODE_PRIVATE);

        videoConvertFirstWrite = true;
        boolean error = false;
        long videoStartTime = startTime;
        long time = System.currentTimeMillis();

        if (resultWidth != 0 && resultHeight != 0) {
            MP4Builder mediaMuxer = null;
            MediaExtractor extractor = null;

            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                Mp4Movie movie = new Mp4Movie();
                movie.setCacheFile(cacheFile);
                movie.setRotation(rotationValue);
                movie.setSize(resultWidth, resultHeight);
                mediaMuxer = new MP4Builder().createMovie(movie);
                extractor = new MediaExtractor();
                extractor.setDataSource(videoPath);

                checkConversionCanceled();

                if (resultWidth != originalWidth || resultHeight != originalHeight || rotateRender != 0) {
                    int videoIndex;
                    videoIndex = selectTrack(extractor, false);
                    if (videoIndex >= 0) {
                        MediaCodec decoder = null;
                        MediaCodec encoder = null;
                        InputSurface inputSurface = null;
                        OutputSurface outputSurface = null;

                        try {
                            long videoTime = -1;
                            boolean outputDone = false;
                            boolean inputDone = false;
                            boolean decoderDone = false;
                            int swapUV = 0;
                            int videoTrackIndex = -5;

                            int colorFormat;
                            int processorType = PROCESSOR_TYPE_OTHER;
                            String manufacturer = Build.MANUFACTURER.toLowerCase();
                            if (Build.VERSION.SDK_INT < 18) {
                                MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
                                colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
                                if (colorFormat == 0) {
                                    throw new RuntimeException("no supported color format");
                                }
                                String codecName = codecInfo.getName();
                                if (codecName.contains("OMX.qcom.")) {
                                    processorType = PROCESSOR_TYPE_QCOM;
                                    if (Build.VERSION.SDK_INT == 16) {
                                        if (manufacturer.equals("lge") || manufacturer.equals("nokia")) {
                                            swapUV = 1;
                                        }
                                    }
                                } else if (codecName.contains("OMX.Intel.")) {
                                    processorType = PROCESSOR_TYPE_INTEL;
                                } else if (codecName.equals("OMX.MTK.VIDEO.ENCODER.AVC")) {
                                    processorType = PROCESSOR_TYPE_MTK;
                                } else if (codecName.equals("OMX.SEC.AVC.Encoder")) {
                                    processorType = PROCESSOR_TYPE_SEC;
                                    swapUV = 1;
                                } else if (codecName.equals("OMX.TI.DUCATI1.VIDEO.H264E")) {
                                    processorType = PROCESSOR_TYPE_TI;
                                }
                                BLog.e("codec = " + codecInfo.getName() + " manufacturer = " + manufacturer + "device = " + Build.MODEL);
                            } else {
                                colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
                            }
                            BLog.e("colorFormat = " + colorFormat);

                            int resultHeightAligned = resultHeight;
                            int padding = 0;
                            int bufferSize = resultWidth * resultHeight * 3 / 2;
                            if (processorType == PROCESSOR_TYPE_OTHER) {
                                if (resultHeight % 16 != 0) {
                                    resultHeightAligned += (16 - (resultHeight % 16));
                                    padding = resultWidth * (resultHeightAligned - resultHeight);
                                    bufferSize += padding * 5 / 4;
                                }
                            } else if (processorType == PROCESSOR_TYPE_QCOM) {
                                if (!manufacturer.toLowerCase().equals("lge")) {
                                    int uvoffset = (resultWidth * resultHeight + 2047) & ~2047;
                                    padding = uvoffset - (resultWidth * resultHeight);
                                    bufferSize += padding;
                                }
                            } else if (processorType == PROCESSOR_TYPE_TI) {
                                //resultHeightAligned = 368;
                                //bufferSize = resultWidth * resultHeightAligned * 3 / 2;
                                //resultHeightAligned += (16 - (resultHeight % 16));
                                //padding = resultWidth * (resultHeightAligned - resultHeight);
                                //bufferSize += padding * 5 / 4;
                            } else if (processorType == PROCESSOR_TYPE_MTK) {
                                if (manufacturer.equals("baidu")) {
                                    resultHeightAligned += (16 - (resultHeight % 16));
                                    padding = resultWidth * (resultHeightAligned - resultHeight);
                                    bufferSize += padding * 5 / 4;
                                }
                            }

                            extractor.selectTrack(videoIndex);
                            if (startTime > 0) {
                                extractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                            } else {
                                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                            }
                            MediaFormat inputFormat = extractor.getTrackFormat(videoIndex);

                            MediaFormat outputFormat = MediaFormat.createVideoFormat(MIME_TYPE, resultWidth, resultHeight);
                            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
                            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate > 0 ? bitrate : 921600);
                            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
                            outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
                            if (Build.VERSION.SDK_INT < 18) {
                                outputFormat.setInteger("stride", resultWidth + 32);
                                outputFormat.setInteger("slice-height", resultHeight);
                            }

                            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
                            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                            if (Build.VERSION.SDK_INT >= 18) {
                                inputSurface = new InputSurface(encoder.createInputSurface());
                                inputSurface.makeCurrent();
                            }
                            encoder.start();

                            decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
                            if (Build.VERSION.SDK_INT >= 18) {
                                outputSurface = new OutputSurface();
                            } else {
                                outputSurface = new OutputSurface(resultWidth, resultHeight, rotateRender);
                            }
                            decoder.configure(inputFormat, outputSurface.getSurface(), null, 0);
                            decoder.start();

                            final int TIMEOUT_USEC = 2500;
                            ByteBuffer[] decoderInputBuffers = null;
                            ByteBuffer[] encoderOutputBuffers = null;
                            ByteBuffer[] encoderInputBuffers = null;
                            if (Build.VERSION.SDK_INT < 21) {
                                decoderInputBuffers = decoder.getInputBuffers();
                                encoderOutputBuffers = encoder.getOutputBuffers();
                                if (Build.VERSION.SDK_INT < 18) {
                                    encoderInputBuffers = encoder.getInputBuffers();
                                }
                            }

                            checkConversionCanceled();

                            while (!outputDone) {
                                checkConversionCanceled();
                                if (!inputDone) {
                                    boolean eof = false;
                                    int index = extractor.getSampleTrackIndex();
                                    if (index == videoIndex) {
                                        int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                                        if (inputBufIndex >= 0) {
                                            ByteBuffer inputBuf;
                                            if (Build.VERSION.SDK_INT < 21) {
                                                inputBuf = decoderInputBuffers[inputBufIndex];
                                            } else {
                                                inputBuf = decoder.getInputBuffer(inputBufIndex);
                                            }
                                            int chunkSize = extractor.readSampleData(inputBuf, 0);
                                            if (chunkSize < 0) {
                                                decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                                inputDone = true;
                                            } else {
                                                decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, extractor.getSampleTime(), 0);
                                                extractor.advance();
                                            }
                                        }
                                    } else if (index == -1) {
                                        eof = true;
                                    }
                                    if (eof) {
                                        int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                                        if (inputBufIndex >= 0) {
                                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                            inputDone = true;
                                        }
                                    }
                                }

                                boolean decoderOutputAvailable = !decoderDone;
                                boolean encoderOutputAvailable = true;
                                while (decoderOutputAvailable || encoderOutputAvailable) {
                                    checkConversionCanceled();
                                    int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                        encoderOutputAvailable = false;
                                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                        if (Build.VERSION.SDK_INT < 21) {
                                            encoderOutputBuffers = encoder.getOutputBuffers();
                                        }
                                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                        MediaFormat newFormat = encoder.getOutputFormat();
                                        if (videoTrackIndex == -5) {
                                            videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
                                        }
                                    } else if (encoderStatus < 0) {
                                        throw new RuntimeException("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                                    } else {
                                        ByteBuffer encodedData;
                                        if (Build.VERSION.SDK_INT < 21) {
                                            encodedData = encoderOutputBuffers[encoderStatus];
                                        } else {
                                            encodedData = encoder.getOutputBuffer(encoderStatus);
                                        }
                                        if (encodedData == null) {
                                            throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                                        }
                                        if (info.size > 1) {
                                            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                                if (mediaMuxer.writeSampleData(videoTrackIndex, encodedData, info, true)) {
                                                    didWriteData(videoObject, cacheFile, false, false);
                                                }
                                            } else if (videoTrackIndex == -5) {
                                                byte[] csd = new byte[info.size];
                                                encodedData.limit(info.offset + info.size);
                                                encodedData.position(info.offset);
                                                encodedData.get(csd);
                                                ByteBuffer sps = null;
                                                ByteBuffer pps = null;
                                                for (int a = info.size - 1; a >= 0; a--) {
                                                    if (a > 3) {
                                                        if (csd[a] == 1 && csd[a - 1] == 0 && csd[a - 2] == 0 && csd[a - 3] == 0) {
                                                            sps = ByteBuffer.allocate(a - 3);
                                                            pps = ByteBuffer.allocate(info.size - (a - 3));
                                                            sps.put(csd, 0, a - 3).position(0);
                                                            pps.put(csd, a - 3, info.size - (a - 3)).position(0);
                                                            break;
                                                        }
                                                    } else {
                                                        break;
                                                    }
                                                }

                                                MediaFormat newFormat = MediaFormat.createVideoFormat(MIME_TYPE, resultWidth, resultHeight);
                                                if (sps != null && pps != null) {
                                                    newFormat.setByteBuffer("csd-0", sps);
                                                    newFormat.setByteBuffer("csd-1", pps);
                                                }
                                                videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
                                            }
                                        }
                                        outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                                        encoder.releaseOutputBuffer(encoderStatus, false);
                                    }
                                    if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                                        continue;
                                    }

                                    if (!decoderDone) {
                                        int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                                        if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                            decoderOutputAvailable = false;
                                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                            MediaFormat newFormat = decoder.getOutputFormat();
                                            BLog.e("newFormat = " + newFormat);
                                        } else if (decoderStatus < 0) {
                                            throw new RuntimeException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                                        } else {
                                            boolean doRender;
                                            if (Build.VERSION.SDK_INT >= 18) {
                                                doRender = info.size != 0;
                                            } else {
                                                doRender = info.size != 0 || info.presentationTimeUs != 0;
                                            }
                                            if (endTime > 0 && info.presentationTimeUs >= endTime) {
                                                inputDone = true;
                                                decoderDone = true;
                                                doRender = false;
                                                info.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                                            }
                                            if (startTime > 0 && videoTime == -1) {
                                                if (info.presentationTimeUs < startTime) {
                                                    doRender = false;
                                                    BLog.e("drop frame startTime = " + startTime + " present time = " + info.presentationTimeUs);
                                                } else {
                                                    videoTime = info.presentationTimeUs;
                                                }
                                            }
                                            decoder.releaseOutputBuffer(decoderStatus, doRender);
                                            if (doRender) {
                                                boolean errorWait = false;
                                                try {
                                                    outputSurface.awaitNewImage();
                                                } catch (Exception e) {
                                                    errorWait = true;
                                                    BLog.e(e);
                                                }
                                                if (!errorWait) {
                                                    if (Build.VERSION.SDK_INT >= 18) {
                                                        outputSurface.drawImage(false);
                                                        inputSurface.setPresentationTime(info.presentationTimeUs * 1000);
                                                        inputSurface.swapBuffers();
                                                    } else {
                                                        int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                                                        if (inputBufIndex >= 0) {
                                                            outputSurface.drawImage(true);
                                                            ByteBuffer rgbBuf = outputSurface.getFrame();
                                                            ByteBuffer yuvBuf = encoderInputBuffers[inputBufIndex];
                                                            yuvBuf.clear();
                                                            //Utilities.convertVideoFrame(rgbBuf, yuvBuf, colorFormat, resultWidth, resultHeight, padding, swapUV);
                                                            encoder.queueInputBuffer(inputBufIndex, 0, bufferSize, info.presentationTimeUs, 0);
                                                        } else {
                                                            BLog.e("input buffer not available");
                                                        }
                                                    }
                                                }
                                            }
                                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                                decoderOutputAvailable = false;
                                                BLog.e("decoder stream end");
                                                if (Build.VERSION.SDK_INT >= 18) {
                                                    encoder.signalEndOfInputStream();
                                                } else {
                                                    int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                                                    if (inputBufIndex >= 0) {
                                                        encoder.queueInputBuffer(inputBufIndex, 0, 1, info.presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (videoTime != -1) {
                                videoStartTime = videoTime;
                            }
                        } catch (Exception e) {
                            BLog.e(e);
                            error = true;
                        }

                        extractor.unselectTrack(videoIndex);

                        if (outputSurface != null) {
                            outputSurface.release();
                        }
                        if (inputSurface != null) {
                            inputSurface.release();
                        }
                        if (decoder != null) {
                            decoder.stop();
                            decoder.release();
                        }
                        if (encoder != null) {
                            encoder.stop();
                            encoder.release();
                        }

                        checkConversionCanceled();
                    }
                } else {
                    long videoTime = readAndWriteTrack(videoObject, extractor, mediaMuxer, info, startTime, endTime, cacheFile, false);
                    if (videoTime != -1) {
                        videoStartTime = videoTime;
                    }
                }
                if (!error && bitrate != -1) {
                    readAndWriteTrack(videoObject, extractor, mediaMuxer, info, videoStartTime, endTime, cacheFile, true);
                }
            } catch (Exception e) {
                error = true;
                BLog.e(e);
            } finally {
                if (extractor != null) {
                    extractor.release();
                }
                if (mediaMuxer != null) {
                    try {
                        mediaMuxer.finishMovie();
                    } catch (Exception e) {
                        BLog.e(e);
                    }
                }
                BLog.e("time = " + (System.currentTimeMillis() - time));
            }
        } else {
            preferences.edit().putBoolean("isPreviousOk", true).commit();
            didWriteData(videoObject, cacheFile, true, true);
            return false;
        }
        preferences.edit().putBoolean("isPreviousOk", true).commit();
        didWriteData(videoObject, cacheFile, true, error);
        return true;
    }

    @TargetApi(16)
    private int selectTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -5;
    }


    private long readAndWriteTrack(final VideoObject videoObject, MediaExtractor extractor, MP4Builder mediaMuxer, MediaCodec.BufferInfo info, long start, long end, File file, boolean isAudio) throws Exception {
        int trackIndex = selectTrack(extractor, isAudio);
        if (trackIndex >= 0) {
            extractor.selectTrack(trackIndex);
            MediaFormat trackFormat = extractor.getTrackFormat(trackIndex);
            int muxerTrackIndex = mediaMuxer.addTrack(trackFormat, isAudio);
            int maxBufferSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            boolean inputDone = false;
            if (start > 0) {
                extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            } else {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
            long startTime = -1;

            checkConversionCanceled();

            while (!inputDone) {
                checkConversionCanceled();

                boolean eof = false;
                int index = extractor.getSampleTrackIndex();
                if (index == trackIndex) {
                    info.size = extractor.readSampleData(buffer, 0);
                    if (Build.VERSION.SDK_INT < 21) {
                        buffer.position(0);
                        buffer.limit(info.size);
                    }
                    if (!isAudio) {
                        byte[] array = buffer.array();
                        if (array != null) {
                            int offset = buffer.arrayOffset();
                            int len = offset + buffer.limit();
                            int writeStart = -1;
                            for (int a = offset; a <= len - 4; a++) {
                                if (array[a] == 0 && array[a + 1] == 0 && array[a + 2] == 0 && array[a + 3] == 1 || a == len - 4) {
                                    if (writeStart != -1) {
                                        int l = a - writeStart - (a != len - 4 ? 4 : 0);
                                        array[writeStart] = (byte) (l >> 24);
                                        array[writeStart + 1] = (byte) (l >> 16);
                                        array[writeStart + 2] = (byte) (l >> 8);
                                        array[writeStart + 3] = (byte) l;
                                        writeStart = a;
                                    } else {
                                        writeStart = a;
                                    }
                                }
                            }
                        }
                    }
                    if (info.size >= 0) {
                        info.presentationTimeUs = extractor.getSampleTime();
                    } else {
                        info.size = 0;
                        eof = true;
                    }

                    if (info.size > 0 && !eof) {
                        if (start > 0 && startTime == -1) {
                            startTime = info.presentationTimeUs;
                        }
                        if (end < 0 || info.presentationTimeUs < end) {
                            info.offset = 0;
                            info.flags = extractor.getSampleFlags();
                            if (mediaMuxer.writeSampleData(muxerTrackIndex, buffer, info, false)) {
                                didWriteData(videoObject, file, false, false);
                            }
                        } else {
                            eof = true;
                        }
                    }
                    if (!eof) {
                        extractor.advance();
                    }
                } else if (index == -1) {
                    eof = true;
                } else {
                    extractor.advance();
                }
                if (eof) {
                    inputDone = true;
                }
            }

            extractor.unselectTrack(trackIndex);
            return startTime;
        }
        return -1;
    }


    private int bitrateTraget = 900000;
    private int bitrateMax = 0;
    private int bitrateMin = 400000;
    private int bitrate = 0;
    private int originalBitrate = 0;
    private long originalSize = 0;
    private float videoDuration = 0;
    private long audioFramesSize = 0;
    private long videoFramesSize = 0;
    private int rotationValue = 0;
    private int originalWidth = 0;
    private int originalHeight = 0;
    private int resultWidth = 0;
    private int resultHeight = 0;

    public static String getFileExt(String fileName) {
        try {

            return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
        } catch (Exception e) {
            return ".";
        }
    }

    boolean noNeedToTransfer = false;

    private boolean processOpenVideo() {
        try {
            File file = new File(sourceVideoPath);
            originalSize = file.length();

            IsoFile isoFile = new IsoFile(sourceVideoPath);
            List<Box> boxes = Path.getPaths(isoFile, "/moov/trak/");
            TrackHeaderBox trackHeaderBox = null;
            boolean isAvc = true;
            boolean isMp4A = true;

            Box boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/mp4a/");
            //if(!getFileExt(sourceVideoPath).contains("mp4"))
            if (boxTest == null) {
                isMp4A = false;
            }
            //if(!getFileExt(sourceVideoPath).contains("mp4"))
            if (!isMp4A) {
                //finishTranscoding(true);
//                GifChecker.setChecked(true);
                noNeedToTransfer = true;
//                return false;
            }
            //if(!getFileExt(sourceVideoPath).contains("mp4"))
            boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/avc1/");
            if (boxTest == null) {
                isAvc = false;
            }

            for (Box box : boxes) {
                TrackBox trackBox = (TrackBox) box;
                long sampleSizes = 0;
                long trackBitrate = 0;
                try {
                    MediaBox mediaBox = trackBox.getMediaBox();
                    MediaHeaderBox mediaHeaderBox = mediaBox.getMediaHeaderBox();
                    SampleSizeBox sampleSizeBox = mediaBox.getMediaInformationBox().getSampleTableBox().getSampleSizeBox();
                    for (long size : sampleSizeBox.getSampleSizes()) {
                        sampleSizes += size;
                    }
                    videoDuration = (float) mediaHeaderBox.getDuration() / (float) mediaHeaderBox.getTimescale();
                    trackBitrate = (int) (sampleSizes * 8 / videoDuration);
                } catch (Exception e) {
                    //FileBLog.e("tmessages", e);
                }
                TrackHeaderBox headerBox = trackBox.getTrackHeaderBox();
                if (headerBox.getWidth() != 0 && headerBox.getHeight() != 0) {
                    trackHeaderBox = headerBox;
                    originalBitrate = bitrate = (int) (trackBitrate / 100000 * 100000);
                    bitrateMax = originalBitrate;
                    if (bitrate > bitrateTraget) {
                        bitrate = bitrateTraget;
                    }
                    videoFramesSize += sampleSizes;
                } else {
                    audioFramesSize += sampleSizes;
                }
            }

            //if(!getFileExt(sourceVideoPath).contains("mp4"))
            if (trackHeaderBox == null) {
                return false;
            }

            Matrix matrix = trackHeaderBox.getMatrix();
            if (matrix.equals(Matrix.ROTATE_90)) {
                rotationValue = 90;
            } else if (matrix.equals(Matrix.ROTATE_180)) {
                rotationValue = 180;
            } else if (matrix.equals(Matrix.ROTATE_270)) {
                rotationValue = 270;
            }
            resultWidth = originalWidth = (int) trackHeaderBox.getWidth();
            resultHeight = originalHeight = (int) trackHeaderBox.getHeight();

            if (resultWidth > 640 || resultHeight > 640) {
                float scale = resultWidth > resultHeight ? 640.0f / resultWidth : 640.0f / resultHeight;
                resultWidth *= scale;
                resultHeight *= scale;
                if (bitrate != 0) {
                    bitrate *= Math.max(0.5f, scale);
                    videoFramesSize = (long) (bitrate / 8 * videoDuration);
                }
            }
            SharedPreferences preferences = VideoApp.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            selectedCompression = preferences.getInt("compress_video2", 1);

            updateWidthHeightBitrateForCompression();
            if (!isAvc && (resultWidth == originalWidth || resultHeight == originalHeight)) {
                return false;
            }
        } catch (Exception e) {
            //FileBLog.e("tmessages", e);
            return false;
        }

        videoDuration *= 1000;
        BLog.d("ffaa", "audiooooooooooooooo = " + audioFramesSize);
        BLog.d("ffaa", "video du1 = " + videoDuration);
        BLog.d("ffaa", "video original size = " + originalWidth + " * " + originalHeight);
        BLog.d("ffaa", "video result size = " + resultWidth + " * " + resultHeight);
        //updateVideoOriginalInfo();
        //updateVideoEditedInfo();

        return true;
    }

    private long startTime = 0;
    private long endTime = 0;
    private int estimatedSize = 0;
    private long esimatedDuration = 0;

    private int updateVideoEditedInfo() {
        int res = 0;
        float right = videoTimelineView.getRightProgress();
        float left = videoTimelineView.getLeftProgress();


        esimatedDuration = (long) Math.ceil((right - left) * videoDuration);

        int width;
        int height;

        if (compressItem == 0 || compressItem == 1 && selectedCompression == compressionsCount - 1) {
            width = rotationValue == 90 || rotationValue == 270 ? originalHeight : originalWidth;
            height = rotationValue == 90 || rotationValue == 270 ? originalWidth : originalHeight;
            estimatedSize = (int) (originalSize * ((float) esimatedDuration / videoDuration));
        } else {
            width = rotationValue == 90 || rotationValue == 270 ? resultHeight : resultWidth;
            height = rotationValue == 90 || rotationValue == 270 ? resultWidth : resultHeight;

            estimatedSize = (int) ((audioFramesSize + videoFramesSize) * ((float) esimatedDuration / videoDuration));
            estimatedSize += estimatedSize / (32 * 1024) * 16;
        }




        if (videoTimelineView.getLeftProgress() == 0) {
            startTime = -1;
        } else {
            startTime = (long) (videoTimelineView.getLeftProgress() * videoDuration) * 1000;
        }
        if (videoTimelineView.getRightProgress() == 1) {
            endTime = -1;
        } else {
            endTime = (long) (videoTimelineView.getRightProgress() * videoDuration) * 1000;
        }

       /* if (videoTimelineView.getLeftProgress() == 0) {
            startTime = -1;
        } else {
            startTime = (long) (videoTimelineView.getLeftProgress() * videoDuration) * 1000;
        }
        if (videoTimelineView.getRightProgress() == 1) {
            endTime = -1;
        } else {
            endTime = (long) (videoTimelineView.getRightProgress() * videoDuration) * 1000;
        }*/


        String videoDimension = String.format("%dx%d", width, height);
        if (esimatedDuration <= 0) {
            finishTranscoding(true);
            return 0;
        }

        int minutes = (int) (esimatedDuration / 1000 / 60);
        int seconds = (int) Math.ceil(esimatedDuration / 1000) - minutes * 60;
        String videoTimeSize = String.format("%d:%02d, ~%s", minutes, seconds, Helper.formatFileSize(estimatedSize));
        title.setText(String.format("%s, %s", videoDimension, videoTimeSize));
        //editedSizeTextView.setText(String.format("%s, %s", videoDimension, videoTimeSize));
        return estimatedSize;
    }

    private int calculateEstimatedSize(float timeDelta) {
        int size = (int) ((audioFramesSize + videoFramesSize) * timeDelta);
        size += size / (32 * 1024) * 16;
        return size;
    }
}
