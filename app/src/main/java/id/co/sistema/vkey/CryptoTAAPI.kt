package id.co.sistema.vkey

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface CryptoTAAPI {

    @POST("encrypt")
    suspend fun verifyMessageenc(
        @Header("jwt") jwt: String
    ): VerifiedStatus

    @POST("decrypt")
    suspend fun verifyMessagedec(
        @Header("jwt") jwt: String
    ): VerifiedStatus

}