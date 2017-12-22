package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.ViewModel
import android.location.Location
import android.location.LocationManager
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.Flowable
import io.reactivex.flowables.ConnectableFlowable
import ru.solodovnikov.rx2locationmanager.LocationTime
import ru.solodovnikov.rx2locationmanager.RxLocationManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LocationViewModel @Inject constructor(private val locationManager: RxLocationManager) : ViewModel() {

    var lastLocation: Location = Location(LocationManager.PASSIVE_PROVIDER)
    val location: ConnectableFlowable<Location> =
            Flowable.interval(0,15, TimeUnit.SECONDS)
                    .flatMapSingle {
                        val gps = locationManager.requestLocation(LocationManager.GPS_PROVIDER, LocationTime(5, TimeUnit.SECONDS))
                        val network = locationManager.requestLocation(LocationManager.NETWORK_PROVIDER, LocationTime(5, TimeUnit.SECONDS))
                        if(lastLocation.provider == LocationManager.PASSIVE_PROVIDER) {
                            network.onErrorReturnItem(lastLocation)
                        } else gps.onErrorResumeNext(network).onErrorReturnItem(lastLocation)
                    }.doOnNext { lastLocation = it }.publish()


    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val locationManager: RxLocationManager) : ViewModelFactory<LocationViewModel>() {

        override val modelClass = LocationViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(this.modelClass)) {
                return LocationViewModel(locationManager) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }

}