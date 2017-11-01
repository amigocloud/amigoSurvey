package com.amigocloud.amigosurvey.form

import android.arch.lifecycle.LiveData
import android.location.Location
import android.location.LocationManager
import javax.inject.Inject

class LocationLiveData @Inject constructor(private val locationManager: LocationManager) : LiveData<Location>() {

}
