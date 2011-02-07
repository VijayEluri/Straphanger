// Copyright 2011 Tom Walsh
//
// This program is free software released under version 3
// of the GPL.  See file gpl.txt for more information.

package com.github.tommywalsh.mbta;


import android.content.Context;
import java.util.AbstractMap;
import java.util.Vector;

public class ProximityTest
{

    // These are approximations that only make sense near Boston
    public static final double latsPerMile = 0.0144578;
    public static final double lngsPerMile = 0.019566791;
    public static final double milesPerMeter = 0.000621;

    static double distanceBetween(double lat1, double lng1, double lat2, double lng2) 
    {
	float[] results = {0.0f};
	android.location.Location.distanceBetween(lat1, lng1,
						  lat2, lng2,
						  results);
	return (double)results[0];
    }

    static private class StopInfoHelper {
	public StopInfo si;
	public double distance;
	public String direction;
    }

    static Vector<StopInfoHelper> getClosestStopHelper(double lat, double lng, RouteInfo ri)
    {
	Vector<StopInfoHelper> stops = new Vector<StopInfoHelper>();
	AbstractMap<String, Vector<StopInfo>> sm = ri.getStopMap();
	for (String dir : sm.keySet()) {
	    StopInfoHelper sih = null;
	    
	    for (StopInfo si : sm.get(dir)) {
		double thisDistance = distanceBetween(lat, lng, si.lat, si.lng);
		if (sih == null || thisDistance < sih.distance) {
		    sih = new StopInfoHelper();
		    sih.si = si;
		    sih.direction = dir;
		    sih.distance = thisDistance;
		}
	    }
	    if (sih != null) {
		stops.addElement(sih);
	    }
	}
	return stops;
    }
   
	
    static Vector<StopInfoHelper> getClosestStops(double lat, double lng, RouteInfo ri)
    {
	return getClosestStopHelper(lat, lng, ri);
    }

    // return null if no stop is within the maximum acceptable distance
    static Vector<StopInfoHelper> getClosestStop(double lat, double lng, RouteInfo ri, double maxDistance)
    {
	double rLat = latsPerMile * maxDistance;
	double minLat = lat - rLat;
	double maxLat = lat + rLat;
	double rLng = lngsPerMile * maxDistance;
	double minLng = lng - rLng;
	double maxLng = lng + rLng;
	   
	// if our acceptable range is entirely higher or lower than the available range,
	// the lat/lng is no good
	boolean latOk = !( (ri.maxLat < minLat) || (maxLat < ri.minLat) );
	boolean lngOk = !( (ri.maxLng < minLng) || (maxLng < ri.minLng) );

	Vector<StopInfoHelper> retval = new Vector<StopInfoHelper>();	
	if (latOk && lngOk) {
	    Vector<StopInfoHelper> candidates = getClosestStopHelper(lat, lng, ri);
	    for (StopInfoHelper sih : candidates) {
		if (sih.distance*0.000621 < maxDistance) {
		    retval.addElement(sih);
		}
	    }
	}
	return retval;
    }




    static void doit(Context context)
    {
	
	for (RouteInfo ri: RouteInfo.getAllRoutes()) {

	    // my house
	    double lat = 42.379159;
	    double lng = -71.099908;
	    
	    for (StopInfoHelper sih : getClosestStop(lat, lng, ri, 0.5)) {
		android.util.Log.d("MBTA", ri.title + ": " + sih.direction + " stops at " + sih.si.title);
	    }
	}

    }
    
}