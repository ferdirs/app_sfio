package id.co.sistema.vkey

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class CryptoTAViewModel: ViewModel(){

    abstract val verifiedStatus: LiveData<VerifiedStatus>

    abstract val isLoading: LiveData<Boolean>
    abstract val isError: LiveData<Event<Unit>>

    abstract fun verifyMessage(jwt: String)



}

class CryptoTAViewModelImpl(
    private val cryptoTAAPI: CryptoTAAPI
): CryptoTAViewModel() {
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Timber.tag("MealViewModel").e(exception.toString())
        _isLoading.value = false
    }

    private val _verifiedStatus = MediatorLiveData<VerifiedStatus>()
    override val verifiedStatus: LiveData<VerifiedStatus>
        get() = _verifiedStatus

    private val _isLoading = MediatorLiveData<Boolean>()
    override val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _isError = MediatorLiveData<Event<Unit>>()
    override val isError: LiveData<Event<Unit>>
        get() = _isError

    override fun verifyMessage(jwt: String) {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val data = cryptoTAAPI.verifyMessageenc(jwt)
                _verifiedStatus.postValue(data)
            }
            _isLoading.value = false
        }
    }


}
