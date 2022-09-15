package id.co.sistema.vkey

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.vkey.android.vguard.VGuard
import com.vkey.android.vguard.VGuardFactory
import com.vkey.vos.signer.taInterface
import id.co.sistema.vkey.databinding.ActivityCryptoTaBinding
import id.co.sistema.vkey.databinding.ActivityMainBinding
import id.co.sistema.vkey.di.dataModule
import id.co.sistema.vkey.di.viewModule
import kotlinx.coroutines.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.startKoin



class CryptoTaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCryptoTaBinding
    var vGuardManager: VGuard? = null

    private val viewModel by viewModel<CryptoTAViewModel>()


    private lateinit var CryptoTA: taInterface
    private lateinit var jwt: String
    private lateinit var signedMessage: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoTaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CoroutineScope(Dispatchers.Main).launch {
            signMessage("")
        }
        if (vGuardManager == null) {
            vGuardManager = VGuardFactory.getInstance()
            Log.d("prosesorrr", "onCreate: ${vGuardManager?.isVosStarted}")}
//        cryptoTaInit()
//        setViewModelObservers()
        binding.btSign.isEnabled = true

        binding.btSign.setOnClickListener {
            val message = """{"message":"${binding.etSignMessage.text.toString()}"}"""
            if (message.isEmpty()) {
                Toast.makeText(this, "Message can not be empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
//            CoroutineScope(Dispatchers.Main).launch {
//                binding.btSign.isEnabled = true
//                signMessage(message)
//            }

            signMessage(message)

        }

    }

    private fun cryptoTaInit() {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                CryptoTA = taInterface.getInstance()
                Log.d("AA","CryptoTA get Instance")

                CryptoTA.loadTA()
                Log.d("AA","CryptoTA Load TA")

                CryptoTA.initialize()
                Log.d("AA","CrtyptoTa Initialize")

                val processManifest = CryptoTA.processManifest(this@CryptoTaActivity)
                Log.d("AA","CryptoTA: ProcessManifest $processManifest")

            }
        }catch (e:Exception){}
    }

    private fun setViewModelObservers() {
        viewModel.isError.observe(this, ::showError)
        viewModel.verifiedStatus.observe(this, ::onDataFetched)
        viewModel.isLoading.observe(this, ::onLoading)
    }


    private fun onLoading(isLoading: Boolean) {
        binding.btSign.isEnabled = !isLoading
    }

    private fun showError(unit: Event<Unit>) {
        Toast.makeText(applicationContext , "Error occured" , Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onDataFetched(data: VerifiedStatus) {
        Toast.makeText(applicationContext , "Verify status: ${data.tokenStatus}" , Toast.LENGTH_SHORT).show()
    }


    private fun signMessage(message: String){
        try {
            CoroutineScope(Dispatchers.IO).launch {
                CryptoTA =taInterface.getInstance()
                Log.d("AA", "CryptoTA get Instance $CryptoTA")

                CryptoTA.loadTA()
                Log.d("AA","CryptoTA Load TA")

                CryptoTA.initialize()
                Log.d("AA","CrtyptoTa Initialize")

                val processManifest =CryptoTA.processManifest(this@CryptoTaActivity)
                Log.d("AA","CryptoTA: ProcessManifest $processManifest")

                if (processManifest<0){
                    Toast.makeText(applicationContext , "manifest failed" , Toast.LENGTH_SHORT).show()
                }else{
                    val error = IntArray(1)
                    val headerEncode = encodeUrlSafe("{\"alg\": \"RS256\",\"typ\": \"JWT\"}".toByteArray())
                    val dataEncode = encodeUrlSafe(message.toByteArray())
                    val payload = "$headerEncode.$dataEncode"
                    val bytes = payload.toByteArray()
                    // 4th argument is "2" to use SHA256. Change to "1" for SHA1
                    val signature = CryptoTA.signMsg(bytes, "Sistema_CA_RSA_2022", 2, error)
                    Log.d("AA","Error Signing value: $error[0]")
                    if (signature == null) {
                        Log.d("AA","Signature Null value: $signature")
                        val unloadTaResult = CryptoTA.unloadTA()
                        Log.d("AA","Unload TA result: $unloadTaResult")
                        Toast.makeText(applicationContext , "aaaa" , Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("AA","Signature NotNull value: $signature")
                        val unloadTaResult = CryptoTA.unloadTA()
                        Log.d("AA","Unload TA result: $unloadTaResult")
                        jwt = payload + "." + encodeUrlSafe(signature)
                        Log.d("AA","Signature JWS value: $jwt")

                    }
                }
            }

        }catch (e:Exception){
            Log.d("AA", "signMessage: $e")
        }
            }
                }


//    }

    fun encodeUrlSafe(data: ByteArray): String {
        val encode: ByteArray =
            Base64.encode(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return String(encode, 0, encode.size)
    }

