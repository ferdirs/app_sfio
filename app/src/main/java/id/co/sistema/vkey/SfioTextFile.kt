package id.co.sistema.vkey

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import com.vkey.android.vguard.*
import com.vkey.securefileio.FileOutputStream
import com.vkey.securefileio.SecureFileIO
import org.json.JSONObject
import vkey.android.vos.Vos
import vkey.android.vos.VosWrapper
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SfioTextFile : AppCompatActivity(), VosWrapper.Callback , VGExceptionHandler {

    private lateinit var mVos: Vos
    private lateinit var mStartVosThread: Thread
    var vGuardManager: VGuard? = null
    private var getFile: File? = null
    private lateinit var encryptedFileLocation: String
    private var isFileEncrypted = false




    // LifecycleHook to notify VGuard of activity's lifecycle
    private lateinit var hook: VGuardLifecycleHook

    // For VGuard to notify host app of events
    private lateinit var broadcastRcvr: VGuardBroadcastReceiver

    companion object {
        private const val TAG = "StringActivity"
        private const val TAG_SFIO = "SecureFileIO"
        private const val STR_INPUT = "Quick brown fox jumps over the lazy dog. 1234567890 some_one@somewhere.com"
        private const val PASSWORD = "P@ssw0rd"
        private const val PROFILE_LOADED = "vkey.android.vguard.PROFILE_LOADED"
        private const val VOS_FIRMWARE_RETURN_CODE_KEY = "vkey.android.vguard.FIRMWARE_RETURN_CODE"
        private const val PROFILE_THREAT_RESPONSE = "vkey.android.vguard.PROFILE_THREAT_RESPONSE"
        private const val TAG_ON_RECEIVE = "OnReceive"
        private const val TAG_VGUARD_STATUS = "VGuardStatus"
        private const val TAG_VOS_READY = "VosReady"
        private const val TAG_VGUARD_MESSAGE = "VguardMessage"
        private const val TAG_HANDLE_THREAT_POLICY = "HandleThreat"
        private const val DEFAULT_VALUE = 0L
        private const val IMAGE_REQUEST_CODE = 100
    }


    private lateinit var et_passenc: EditText
    private lateinit var et_up_text: EditText
    private lateinit var et_up_pass: EditText
    private lateinit var et_pass_dec: EditText

    private lateinit var bt_enc: Button
    private lateinit var bt_upt: Button
    private lateinit var bt_dec: Button

    private lateinit var tv_res: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sfio_text_file)

        mVos = Vos(this)
        mVos.registerVosWrapperCallback(this)
        startVos(this)
        setupVGuard(this)

        prepareFiles()

        et_passenc = findViewById(R.id.et_pass_enc)
        et_up_text = findViewById(R.id.et_up_text)
        et_up_pass = findViewById(R.id.et_up_pass)
        et_pass_dec = findViewById(R.id.et_pass_dec)

        bt_enc = findViewById(R.id.bt_enc_file)
        bt_upt = findViewById(R.id.bt_updating)
        bt_dec = findViewById(R.id.bt_dec)

        tv_res = findViewById(R.id.tv_dec_res)

        bt_enc.setOnClickListener {
            enc()
        }

        bt_upt.setOnClickListener {
            update()
        }

        bt_dec.setOnClickListener {
            dec()
        }

    }

    private fun enc(){
        if (et_passenc.text.isEmpty()){
            Toast.makeText(this , "Passowrd" , Toast.LENGTH_SHORT).show()
            return
        }
        if (!isFileEncrypted){
            existingFile()
        }
        try {
            val pass = et_passenc.text.toString() + "P@ssw0rd"
            SecureFileIO.encryptFile(encryptedFileLocation , pass)
            et_passenc.text.clear()
            Toast.makeText(this , "File Encrypted" , Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            Toast.makeText(this , "File Failed Encrypted" , Toast.LENGTH_SHORT).show()
        }
    }

    private fun existingFile(){
        try {
            java.io.FileOutputStream(encryptedFileLocation).use {
                it.write(STR_INPUT.toByteArray())
                it.close()
            }
            Toast.makeText(this , "file overwrite" , Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            Toast.makeText(this , "file overwrite failed" , Toast.LENGTH_SHORT).show()
        }
    }

    private fun dec(){
        if (et_pass_dec.text.isEmpty()){
            Toast.makeText(this , "inputpassword" , Toast.LENGTH_SHORT).show()
        }
        try {
            val pass = et_pass_dec.text.toString() + "P@ssw0rd"
            val txtString =com.vkey.securefileio.FileInputStream(encryptedFileLocation , pass)
                .bufferedReader().use { it.readText() }

                tv_res.text =txtString
                et_pass_dec.text.clear()
                Toast.makeText(this , "Decrypted" , Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            Toast.makeText(this , "$e" , Toast.LENGTH_SHORT).show()

        }
    }

    private fun update(){
        if (et_up_text.text.isEmpty() && et_up_pass.text.isEmpty()){
            Toast.makeText(this , "Text and password must be filled" , Toast.LENGTH_SHORT).show()
        }
        try {
            val data =et_up_text.text.toString()
            val pass = et_up_pass.text.toString() + "P@ssw0rd"
            com.vkey.securefileio.FileOutputStream(encryptedFileLocation , pass).use {
                it.write(data.toByteArray())
                it.close()
            }
            clearField()
            Toast.makeText(this , "Content is updated" , Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            Log.d("bugg", "update: $e")
        }
    }

    private fun clearField() {
        et_up_text.text.clear()
        et_up_pass.text.clear()
    }

    private fun startVos(ctx: Context) {
        mStartVosThread = Thread {
            try {
                // Get the kernel data in byte from `firmware` asset file
                val inputStream = ctx.assets.open("firmware")
                val kernelData = inputStream.readBytes()
                inputStream.read(kernelData)
                inputStream.close()

                // Start V-OS
                val vosReturnCode = mVos.start(kernelData, null, null, null, null)

                if (vosReturnCode > 0) {
                    // Successfully started V-OS
                    // Instantiate a `VosWrapper` instance for calling V-OS Processor APIs
                    if (vGuardManager == null) {
                        vGuardManager = VGuardFactory.getInstance()}

                    val vosWrapper = VosWrapper.getInstance(ctx)
                    val version = vosWrapper.processorVersion
                    val troubleShootingID = String(vosWrapper.troubleshootingId)

                    Log.d(
                        SfioTextFile.TAG,
                        "ProcessorVers: $version || TroubleShootingID: $troubleShootingID"
                    )
                } else {
                    // Failed to start V-OS
                    Log.e(SfioTextFile.TAG, "Failed to start V-OS")
                }
            } catch (e: VGException) {
                Log.e(SfioTextFile.TAG, e.message.toString())
                e.printStackTrace()
            }
        }

        mStartVosThread.start()
    }

    private fun setupVGuard(activity: Activity){
        receiveVGuardBroadcast(activity)
        registerLocalBroadcast()
        setupAppProtection()
    }

    private fun receiveVGuardBroadcast(activity: Activity){
        broadcastRcvr = object : VGuardBroadcastReceiver(activity) {
            override fun onReceive(context: Context?, intent: Intent?) {
                super.onReceive(context, intent)

                when {
                    SfioTextFile.PROFILE_LOADED == intent?.action -> Log.d(
                        SfioTextFile.TAG_ON_RECEIVE,
                        "PROFILE_LOADED"
                    )

                    ACTION_SCAN_COMPLETE == intent?.action ->
                        onScanComplete(intent)


                    VGUARD_OVERLAY_DETECTED_DISABLE == intent?.action -> Log.d(
                        SfioTextFile.TAG_ON_RECEIVE,
                        "VGUARD_OVERLAY_DETECTED_DISABLE"
                    )

                    VGUARD_OVERLAY_DETECTED == intent?.action -> Log.d(
                        SfioTextFile.TAG_ON_RECEIVE,
                        "VGUARD_OVERLAY_DETECTED"
                    )

                    VGUARD_STATUS == intent?.action -> {
                        Log.d(
                            SfioTextFile.TAG_VGUARD_STATUS,
                            "HasExtraVGuardInitStatus: ${intent.hasExtra(VGUARD_INIT_STATUS)}"
                        )

                        if (intent.hasExtra(VGUARD_MESSAGE)) {
                            val message = intent.getStringExtra(VGUARD_MESSAGE)
                            var allMessage = "\n $VGUARD_MESSAGE : $message"
                            if (message != null) {
                                Log.d("MSG", message)
                            }
                            Log.d(SfioTextFile.TAG_VGUARD_MESSAGE, allMessage)
                        }

                        if (intent.hasExtra(VGUARD_HANDLE_THREAT_POLICY)) {
                            val detectedThreats =
                                intent.getParcelableArrayListExtra<Parcelable>(SCAN_COMPLETE_RESULT)
                            val builder = StringBuilder()

                            if (detectedThreats != null) {
                                for (info in detectedThreats) {
                                    val infoStr = (info as BasicThreatInfo).toString()
                                    builder.append("$infoStr \n")
                                }

                                val highestResponse =
                                    intent.getIntExtra(VGUARD_HIGHEST_THREAT_POLICY, -1)
                                val alertTitle = intent.getStringExtra(VGUARD_ALERT_TITLE)
                                val alertMessage = intent.getStringExtra(VGUARD_ALERT_MESSAGE)
                                val disabledAppExpired =
                                    intent.getLongExtra(VGUARD_DISABLED_APP_EXPIRED, 0)

                                when {
                                    highestResponse > 0 -> builder.append("highest policy: $highestResponse\n")
                                    !TextUtils.isEmpty(alertTitle) -> builder.append("alertTitle: $alertTitle\n")
                                    !TextUtils.isEmpty(alertMessage) -> builder.append("alertMessage: $alertMessage\n")
                                    disabledAppExpired > 0 -> {
                                        val format = SimpleDateFormat(
                                            "yyyy-MMdd HH:mm:ss",
                                            Locale.getDefault()
                                        )
                                        val activeDate = format.format(Date(disabledAppExpired))
                                        builder.append("App can use again after: $activeDate\n")
                                    }
                                }

                                Log.d(SfioTextFile.TAG_HANDLE_THREAT_POLICY, builder.toString())
                            }
                        }

                        if (intent.hasExtra(VGUARD_INIT_STATUS)) {
                            Log.d(
                                SfioTextFile.TAG_VGUARD_STATUS,
                                "VGUARD_INIT_STATUS: ${
                                    intent.getBooleanExtra(
                                        VGUARD_INIT_STATUS,
                                        false
                                    )
                                }"
                            )
                            val initStatus = intent.getBooleanExtra(VGUARD_INIT_STATUS, false)
                            var message = "\n $VGUARD_STATUS: $initStatus"

                            if (!initStatus) {
                                try {
                                    val jsonObject =
                                        JSONObject(intent.getStringExtra(VGUARD_MESSAGE))
                                    Log.d(
                                        SfioTextFile.TAG_VGUARD_STATUS,
                                        "code: ${jsonObject.getString("code")}"
                                    )
                                    Log.d(
                                        SfioTextFile.TAG_VGUARD_STATUS,
                                        "code: ${jsonObject.getString("description")}"
                                    )
                                    message += jsonObject.toString()
                                } catch (e: java.lang.Exception) {
                                    Log.e(SfioTextFile.TAG_VGUARD_STATUS, e.message.toString())
                                    e.printStackTrace()
                                }
                                Log.d(SfioTextFile.TAG_VGUARD_STATUS, message)
                            }
                        }

                        if (intent.hasExtra(VGUARD_SSL_ERROR_DETECTED)) {
                            Log.d(
                                SfioTextFile.TAG_VGUARD_STATUS,
                                "VGUARD_SSL_ERROR_DETECTED: ${
                                    intent.getBooleanExtra(
                                        VGUARD_SSL_ERROR_DETECTED,
                                        false
                                    )
                                }"
                            )
                            val sslError = intent.getBooleanExtra(VGUARD_SSL_ERROR_DETECTED, false)
                            var message = "\n $VGUARD_SSL_ERROR_DETECTED: $sslError"

                            if (sslError) {
                                try {
                                    val jsonObject =
                                        JSONObject(intent.getStringExtra(VGUARD_MESSAGE))
                                    Log.d(
                                        SfioTextFile.TAG_VGUARD_STATUS,
                                        jsonObject.getString(VGUARD_ALERT_TITLE)
                                    )
                                    Log.d(
                                        SfioTextFile.TAG_VGUARD_STATUS,
                                        jsonObject.getString(VGUARD_ALERT_MESSAGE)
                                    )
                                    message += jsonObject.toString()
                                } catch (e: java.lang.Exception) {
                                    Log.e(SfioTextFile.TAG_VGUARD_STATUS, e.message.toString())
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    VOS_READY == intent?.action -> {
                        val firmwareReturnCode =
                            intent.getLongExtra(
                                SfioTextFile.VOS_FIRMWARE_RETURN_CODE_KEY,
                                SfioTextFile.DEFAULT_VALUE
                            )
                        if (firmwareReturnCode >= SfioTextFile.DEFAULT_VALUE) {
                            // if the `VGuardManager` is not available,
                            // create a `VGuardManager` instance from `VGuardFactory`
                            if (vGuardManager == null) {
                                vGuardManager = VGuardFactory.getInstance()
                                hook = ActivityLifecycleHook(vGuardManager)

                                val isStarted = vGuardManager?.isVosStarted.toString()
                                val valueTID = vGuardManager?.troubleshootingId.toString()



                                Log.d(SfioTextFile.TAG_VOS_READY, "isVosStarted: $isStarted")
                                Log.d(SfioTextFile.TAG_VOS_READY, "TID: $valueTID")
                            }
                        } else {
                            // Error handling
                            Log.d(SfioTextFile.TAG_VOS_READY, "vos_ready_error_firmware")
                        }
                        Log.d(SfioTextFile.TAG_VOS_READY, "VOS_READY")
                    }
                }
            }
        }
    }

    private fun onScanComplete(intent: Intent?){
        val detectThreat = intent?.getParcelableArrayListExtra<Parcelable>(
            VGuardBroadcastReceiver.SCAN_COMPLETE_RESULT
        ) as ArrayList<Parcelable>

        val threat: ArrayList<BasicThreatInfo> = arrayListOf()
        for(item in detectThreat){
            threat.add(item as BasicThreatInfo)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            org.greenrobot.eventbus.EventBus.getDefault().post(threat)
        }, 3000L)
        Log.d(
            SfioTextFile.TAG_ON_RECEIVE,
            "ACTION_SCAN_COMPLETE"
        )
    }




    private fun registerLocalBroadcast(){
        LocalBroadcastManager.getInstance(this).apply {
            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.ACTION_FINISH))
            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.ACTION_SCAN_COMPLETE))
            registerReceiver(broadcastRcvr, IntentFilter(SfioTextFile.PROFILE_LOADED))
            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.VOS_READY))
            registerReceiver(broadcastRcvr, IntentFilter(SfioTextFile.PROFILE_THREAT_RESPONSE))
            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.VGUARD_OVERLAY_DETECTED))
            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.VGUARD_OVERLAY_DETECTED_DISABLE))
            registerReceiver(broadcastRcvr, IntentFilter(VGuardBroadcastReceiver.VGUARD_STATUS))
        }
    }

    private fun setupAppProtection(){
        try {
            val config = VGuardFactory.Builder()
                .setDebugable(true)
                .setAllowsArbitraryNetworking(true)
                .setMemoryConfiguration(MemoryConfiguration.DEFAULT)
                .setVGExceptionHandler(this)

            VGuardFactory().getVGuard(this, config)
        } catch (e: java.lang.Exception) {
            Log.e(SfioTextFile.TAG, e.message.toString())
            e.printStackTrace()
        }
    }

    override fun onNotified(p0: Int, p1: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun handleException(e: Exception?) {
        TODO("Not yet implemented")
    }

    private fun prepareFiles() {
        val dirLocation = File("${this.filesDir.absolutePath}/test")
        if (!dirLocation.exists()) dirLocation.mkdir()

        encryptedFileLocation = "$dirLocation/existingFile.txt"
        val file = File(encryptedFileLocation)
        if (!file.exists()) {
            file.createNewFile()
            Log.d("newfile", "prepareFiles: ")
        } else {
            Log.d("existfile", "prepareFiles: ")
        }
    }
}