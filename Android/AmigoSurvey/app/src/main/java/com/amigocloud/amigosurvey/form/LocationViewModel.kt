package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.ViewModel
import android.location.Location
import android.location.LocationManager
import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.flowables.ConnectableFlowable
import ru.solodovnikov.rx2locationmanager.LocationTime
import ru.solodovnikov.rx2locationmanager.RxLocationManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

//class LocationLiveData @Inject constructor(private val locationManager: LocationManager) : LiveData<Location>()

@Singleton
class LocationViewModel @Inject constructor(private val locationManager: RxLocationManager) : ViewModel() {

    var lastLocationSingle: Single<Location> = Single.just(Location(LocationManager.PASSIVE_PROVIDER))
    var lastLocation: Location = Location(LocationManager.PASSIVE_PROVIDER)

    val location: ConnectableFlowable<Location>? =
            Flowable.timer(3, TimeUnit.SECONDS)
                    .repeat()
                    .flatMapSingle {
                        lastLocationSingle = locationManager.requestLocation(LocationManager.GPS_PROVIDER,
                                LocationTime(3, TimeUnit.SECONDS))
                                .onErrorReturn {
                                    lastLocationSingle = locationManager.requestLocation(LocationManager.NETWORK_PROVIDER,
                                            LocationTime(3, TimeUnit.SECONDS))
                                            .onErrorReturn {
                                                lastLocationSingle.blockingGet()
                                            }
                                    lastLocation = lastLocationSingle.blockingGet()
                                    lastLocation
                                }
                        lastLocation = lastLocationSingle.blockingGet()
                        lastLocationSingle
                    }.publish()

}