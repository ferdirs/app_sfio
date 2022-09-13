package id.co.sistema.vkey

import com.google.gson.annotations.SerializedName

data class VerifiedStatus(
    @SerializedName("token_status")
    val tokenStatus: Boolean
)
