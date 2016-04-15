/**
 * 
 */
package edu.sjsu.cmpe295.gearnavi.event;

import java.io.File;

/**
 * @author mly
 *
 */
public class MapTileReadyEvent {
    public final String filename;
    public final String path;
    
    public MapTileReadyEvent(final String filename) {
        this(filename, null);
    }
    
    public MapTileReadyEvent(final String filename, final String path) {
        this.filename = filename;
        if(path == null) {
            this.path = File.separator;
        }
        else {
            this.path = path;
        }
    }
}
