package id.co.sistema.vkey.di

import id.co.sistema.vkey.BuildConfig
import id.co.sistema.vkey.CryptoTAAPI
import id.co.sistema.vkey.RetrofitFactory
import org.koin.core.qualifier.named
import org.koin.dsl.module

val BASE_URL_CRYPTO_TA = "http://cryptotademo.herokuapp.com/"


val dataModule = module {


    single {
        RetrofitFactory.provideHttpLoggingInterceptor(BuildConfig.DEBUG)
    }

    single {
        RetrofitFactory.provideOkHttpClientBuilder(get())
    }

    single(named("baseUrlCryptoTA")) {
        BASE_URL_CRYPTO_TA
    }


    single(named("builderCryptoTA")) {
        RetrofitFactory.provideRetrofitBuilder(get(named("baseUrlCryptoTA")))
    }

    single(named("builderMeal")) {
        RetrofitFactory.provideRetrofitBuilder(get(named("baseUrlMeal")))
    }

    single {
        RetrofitFactory.provideService(
            CryptoTAAPI::class.java, get(), get(named("builderCryptoTA"))
        )
    }



}