package id.co.sistema.vkey

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import id.co.sistema.vkey.databinding.ActivityCryptoTaBinding
import id.co.sistema.vkey.databinding.ActivityMainBinding

class CryptoTaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCryptoTaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoTaBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}