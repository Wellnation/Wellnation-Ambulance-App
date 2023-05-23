package com.wellnation.ambulanceportal

import com.google.firebase.firestore.GeoPoint

class ambulanceData {
    companion object{
        val authpass: String = ""
        var contact: String = ""
        val cost: Int = 0
        val currentlocation: GeoPoint = GeoPoint(0.0, 0.0)
        var driverName: String = ""
        val dropLocation: GeoPoint = GeoPoint(0.0, 0.0)
        var id: String = ""
        val pickupStatus: Boolean = false
        val pickuplocation: GeoPoint = GeoPoint(0.0, 0.0)
        val status: Boolean = false
        var vechilenumber: String = ""
        var patientcontactnumber:String = ""
//var ambulancedata = Ambulance()
    }
}