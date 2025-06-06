package com.coordinadora.pruebavideocam.application.dagger

import com.coordinadora.pruebavideocam.database.AppDatabase
import com.coordinadora.pruebavideocam.ui.MainActivity
import com.coordinadora.pruebavideocam.ui.Page2
import com.coordinadora.pruebavideocam.ui.Page3
import com.coordinadora.pruebavideocam.utils.Connectivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [PruebasCamModule::class])
interface PruebasCamComponent {
    fun inject(pruebasCamApplication: PruebasCamApplication)
    fun inject(mainActivity: MainActivity)
    fun AppDatabase(): AppDatabase
    fun PruebasCamApplication(): PruebasCamApplication
    fun inject(page2: Page2)
    fun inject(page3: Page3)
    fun inject(connectivity: Connectivity)

}