/**
 * 
 */
package edu.sjsu.cmpe295.gearnavi.event;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * @author mly
 *
 */
public class EventBusProvider {
    
    private static final Bus BUS = new Bus(ThreadEnforcer.ANY);

    private EventBusProvider() {}
    
    public static Bus getInstance() {
        return BUS;
    }
}