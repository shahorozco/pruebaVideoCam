package com.coordinadora.pruebavideocam.application.dagger

import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

class PruebasCamApplication: MultiDexApplication() {
    private lateinit var application: PruebasCamApplication
    private lateinit var PruebasCamComponent: PruebasCamComponent

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        this.application = this
        PruebasCamComponent = DaggerPruebasCamComponent.builder().pruebasCamModule(PruebasCamModule(this)).build()
        PruebasCamComponent.inject(this)
    }

    fun getPruebasCamComponent(): PruebasCamComponent {
        return this.PruebasCamComponent
    }
}