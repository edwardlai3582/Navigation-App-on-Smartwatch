/**
 * 
 */
package edu.sjsu.cmpe295.gearnavi.event;

import android.location.Location;

/**
 * @author mly
 *
 */
public class LocationChangedEvent {
    
    public final Location location;
    
    public LocationChangedEvent(final Location location) {
        this.location = location;
    }
}