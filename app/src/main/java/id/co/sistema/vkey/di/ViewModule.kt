package id.co.sistema.vkey.di

import id.co.sistema.vkey.CryptoTAViewModel
import id.co.sistema.vkey.CryptoTAViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModule = module {


    viewModel<CryptoTAViewModel> {
        CryptoTAViewModelImpl(get())
    }
}