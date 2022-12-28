package com.example.login

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue


/**
 * Copyright (c) 2019 by Roman Sisik. All rights reserved.
 */
class CamService: Service() {
    private lateinit var videoName:String

    private var areaName: String = ""
    private var userId: String = ""
    private var idIntv: String = ""

    // UI
    private var wm: WindowManager? = null
    private var rootView: View? = null
    private var textureView: TextureView? = null
    private var switchCamera: ImageView? = null

    // Camera2-related stuff
    private var cameraManager: CameraManager? = null
    private var previewSize: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mCurrentFile: File? = null
    private var mIsRecordingVideo: Boolean = false
    // You can start service in 2 modes - 1.) with preview 2.) without preview (only bg processing)
    private var shouldShowPreview = true

    private val helper: FirebaseHelper = FirebaseHelper()

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {}

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {}
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            initCam(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }


    private val imageListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader?.acquireLatestImage()

        Log.d(TAG, "Got image: " + image?.width + " x " + image?.height)

        // Process image here..ideally async so that you don't block the callback
        // ..

        image?.close()
    }

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(currentCameraDevice: CameraDevice) {
            cameraDevice = currentCameraDevice

            createCaptureSession()
            Log.d("erza", "camera an open")
        }

        override fun onDisconnected(currentCameraDevice: CameraDevice) {
            if(!mIsRecordingVideo){
                Log.d("erza", "disconnect")
                currentCameraDevice.close()
                cameraDevice = null
            }else{
                Log.d("erza", "disconnect but still record")
                reopencamera(currentCameraDevice.id,320,200)
            }

        }

        override fun onError(currentCameraDevice: CameraDevice, error: Int) {
            Log.d("erza", "camera error $error")
            currentCameraDevice.close()
            cameraDevice = null
        }
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        helper.initFirebase()
        when(intent?.action) {
            ACTION_START ->{
                videoName = intent?.getStringExtra("VIDEO_NAME").orEmpty()
                areaName = intent?.getStringExtra("AREA_NAME").orEmpty()
                userId = intent?.getStringExtra("USER_ID").orEmpty()
                start()
            }

            ACTION_START_WITH_PREVIEW ->{
                videoName = intent?.getStringExtra("VIDEO_NAME").orEmpty()
                areaName = intent?.getStringExtra("AREA_NAME").orEmpty()
                userId = intent?.getStringExtra("USER_ID").orEmpty()
                idIntv = intent?.getStringExtra("ID_INTV").orEmpty()
                startWithPreview()
            }


            "STOP_SERVICE"->{
                stopForeground(true)
                stopSelf()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        startForeground()
    }

    override fun onDestroy() {

        super.onDestroy()

        Log.d("erza", "camera onDestroy")
        stopCamera()
        stopRecordingVideo()

        if (rootView != null)
            wm?.removeView(rootView)

        sendBroadcast(Intent(ACTION_STOPPED))
    }

    private fun start() {

        shouldShowPreview = false

        initCam(320, 200)
    }

    private fun startWithPreview() {

        shouldShowPreview = true

        // Initialize view drawn over other apps
        initOverlay()

        // Initialize camera here if texture view already initialized
        if (textureView!!.isAvailable)
            initCam(textureView!!.width, textureView!!.height)
        else
            textureView!!.surfaceTextureListener = surfaceTextureListener
    }

    private fun initOverlay() {

        val li = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        rootView = li.inflate(R.layout.overlay, null)
        textureView = rootView?.findViewById(R.id.texPreview)
        switchCamera = rootView?.findViewById<ImageView>(R.id.switchcamera)

        val type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm!!.addView(rootView, params)
    }

    private fun initCam(width: Int, height: Int) {

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var camId: String? = null
        var facing: Int? = CameraCharacteristics.LENS_FACING_FRONT
        for (id in cameraManager!!.cameraIdList) {
            val characteristics = cameraManager!!.getCameraCharacteristics(id)
            facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                camId = id
                break
            }
        }

        switchCamera?.setOnClickListener {
            if(facing == CameraCharacteristics.LENS_FACING_BACK) {
                facing = CameraCharacteristics.LENS_FACING_FRONT
        } else if(facing == CameraCharacteristics.LENS_FACING_FRONT) {
            facing = CameraCharacteristics.LENS_FACING_BACK}
        }

        reopencamera(camId, width, height)
    }

    private fun reopencamera(camId: String?, width:Int,height: Int){
        if (textureView?.isAvailable == true){
            previewSize = chooseSupportedSize(camId!!, width, height)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            cameraManager!!.openCamera(camId, stateCallback, null)
        }
    }

    private fun chooseSupportedSize(camId: String, textureViewWidth: Int, textureViewHeight: Int): Size {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Get all supported sizes for TextureView
        val characteristics = manager.getCameraCharacteristics(camId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = map?.getOutputSizes(SurfaceTexture::class.java)

        // We want to find something near the size of our TextureView
        val texViewArea = textureViewWidth * textureViewHeight
        val texViewAspect = textureViewWidth.toFloat()/textureViewHeight.toFloat()

        val nearestToFurthestSz = supportedSizes?.sortedWith(compareBy(
            // First find something with similar aspect
            {
                val aspect = if (it.width < it.height) it.width.toFloat() / it.height.toFloat()
                else it.height.toFloat()/it.width.toFloat()
                (aspect - texViewAspect).absoluteValue
            },
            // Also try to get similar resolution
            {
                (texViewArea - it.width * it.height).absoluteValue
            }
        ))


        if (nearestToFurthestSz.isNullOrEmpty().not())
            return nearestToFurthestSz?.get(0) ?:Size(0,0)

        return Size(320, 200)
    }

    private fun startForeground() {

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, getPendingIntentFlags())
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(this, CamService::class.java)
        intent.putExtra("AREA_NAME", areaName)
        intent.putExtra("USER_ID", userId)
        intent.setAction("STOP_SERVICE")
        val stopService: PendingIntent = PendingIntent.getService(this, 0,intent,getPendingIntentFlags())
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.app_name))
            .addAction(0,"Stop Record",stopService)
            .setSmallIcon(androidx.transition.R.drawable.notification_template_icon_bg)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.app_name))
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun getPendingIntentFlags():Int{
        var flags= 0
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)
        {
            flags=PendingIntent.FLAG_IMMUTABLE
        }
        return flags
    }

    private fun createCaptureSession() {
        try {
            setUpMediaRecorder()


            // Prepare surfaces we want to use in capture session
            val targetSurfaces = ArrayList<Surface>()

            // Prepare CaptureRequest that can be used with CameraCaptureSession
            val requestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {

                if (shouldShowPreview) {
                    val texture = textureView!!.surfaceTexture!!
                    texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
                    val previewSurface = Surface(texture)

                    targetSurfaces.add(previewSurface)
                    addTarget(previewSurface)
                }

                // Configure target surface for background processing (ImageReader)
                imageReader = ImageReader.newInstance(
                    previewSize!!.getWidth(), previewSize!!.getHeight(),
                    ImageFormat.YUV_420_888, 2
                )
                imageReader!!.setOnImageAvailableListener(imageListener, null)

                targetSurfaces.add(imageReader!!.surface)
                addTarget(imageReader!!.surface)

                // Set some additional parameters for the request
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }
            //MediaRecorder setup for surface
            val recorderSurface = mMediaRecorder?.surface
            if (recorderSurface!=null){
                targetSurfaces.add(recorderSurface)
                requestBuilder.addTarget(recorderSurface)

            }

            // Prepare CameraCaptureSession
            cameraDevice!!.createCaptureSession(targetSurfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }

                        captureSession = cameraCaptureSession
                        try {

                            // Now we can start capturing
                            captureRequest = requestBuilder!!.build()
                            captureSession?.setRepeatingRequest(captureRequest!!, captureCallback, null)

                            startRecordingVideo()
                        } catch (e: CameraAccessException) {
                            Toast.makeText(applicationContext,"camera failed ${e.message}", Toast.LENGTH_LONG).show()
                            Log.d("erza", "camera failed ${e.message}")
                            Log.e(TAG, "createCaptureSession", e)
                        }

                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(applicationContext,"configure failed", Toast.LENGTH_LONG).show()
                        Log.d("erza", "configure failed")
                        Log.e(TAG, "createCaptureSession()")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.d("erza", "camera access failed ${e.message}")
            Log.e(TAG, "createCaptureSession", e)
        }
    }
    private fun startRecordingVideo(){
        mIsRecordingVideo = true
        // Start recording
        mMediaRecorder?.start()
        Toast.makeText(applicationContext,"start record", Toast.LENGTH_LONG).show()
        Log.d("erza", "start record")
    }

    private fun stopCamera() {

        try {


            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            mMediaRecorder?.release()
            mMediaRecorder = null


        } catch (e: Exception) {
            Log.d("erza", "camera stop ${e.message}")
            e.printStackTrace()
        }
    }

    @Throws(java.lang.Exception::class)
    fun stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false
        try {
            val output = mCurrentFile
            output?.let {
                file->
                helper.uploadVideo(
                    videoPath = file.path,
                    area = areaName,
                    onFailure = {
                        Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                        Log.d("cam", it)
                    },
                    onSuccess = {
                            videoUrl ->
                        helper.saveDataVideo(
                            id_user = userId,
                            title = file.name, videoUrl = videoUrl,
                            onSuccess = {
                                Toast.makeText(applicationContext, "Success", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = {
                                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                                Log.d("cam", it+"save video")
                            }
                        )
                    }
                )
            }

            Toast.makeText(applicationContext,"stop record", Toast.LENGTH_LONG).show()
            Log.d("erza", "stop record")
            captureSession?.stopRepeating()
            captureSession?.abortCaptures()
        } catch (e: CameraAccessException) {
            Toast.makeText(applicationContext,"stop failed ${e.message}", Toast.LENGTH_LONG).show()
            Log.d("erza", "stop failed ${e.message}")
            e.printStackTrace()
        }
        // Stop recording
        mMediaRecorder?.stop()
        mMediaRecorder?.reset()
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        mMediaRecorder = MediaRecorder()
        mMediaRecorder?.setOrientationHint(90)
        mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        /**
         * create video output file
         */
        mCurrentFile = getOutputMediaFile()
        /**
         * set output file in media recorder
         */
        mMediaRecorder?.setOutputFile(mCurrentFile?.getAbsolutePath())
        Toast.makeText(applicationContext,"file save ${mCurrentFile?.absolutePath}", Toast.LENGTH_LONG).show()
        Log.d("erza", "file save ${mCurrentFile?.absolutePath}")
        if(CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH_SPEED_HIGH)){
            val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH_SPEED_HIGH)
            mMediaRecorder?.setVideoFrameRate(profile.videoFrameRate)
            mMediaRecorder?.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
            mMediaRecorder?.setVideoEncodingBitRate(profile.videoBitRate)
            mMediaRecorder?.setAudioEncodingBitRate(profile.audioBitRate)
            mMediaRecorder?.setAudioSamplingRate(profile.audioSampleRate)
        }

        mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)


        try{
            mMediaRecorder?.prepare()
        }
        catch (e:Exception
        ){
            Log.d("erza", "error prepare ${e.message}" )
        }
    }

    private fun getOutputMediaFile(): File? {
        // External sdcard file location
        val mediaStorageDir =
            getExternalFilesDir(VIDEO_DIRECTORY_NAME)

        // Create storage directory if it does not exist
        if (mediaStorageDir?.exists() == false) {
            if (mediaStorageDir?.mkdirs() == false) {
                Log.d(
                    TAG, ("Oops! Failed create "
                            + VIDEO_DIRECTORY_NAME) + " directory"
                )
                return null
            }
        }
        val timeStamp: String = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        val mediaFile: File
        mediaFile = File(
            mediaStorageDir?.path + File.separator
                    + videoName + timeStamp + ".mp4"
        )
        return mediaFile
    }





    companion object {

        val TAG = "CamService"

        val ACTION_START = "eu.sisik.backgroundcam.action.START"
        val ACTION_START_WITH_PREVIEW = "eu.sisik.backgroundcam.action.START_WITH_PREVIEW"
        val ACTION_STOPPED = "eu.sisik.backgroundcam.action.STOPPED"

        val ONGOING_NOTIFICATION_ID = 6660
        val CHANNEL_ID = "cam_service_channel_id"
        val CHANNEL_NAME = "cam_service_channel_name"
        val VIDEO_DIRECTORY_NAME = "Erza"

    }


}