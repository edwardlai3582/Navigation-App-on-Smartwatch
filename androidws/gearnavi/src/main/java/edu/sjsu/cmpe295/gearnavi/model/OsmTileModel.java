/**
 * 
 */
package edu.sjsu.cmpe295.gearnavi.model;

import java.io.File;

/**
 * @author mly
 * 
 */
public class OsmTileModel {
    public static final byte MIN_ZOOM_VAL = 16;
    public static final byte MAX_ZOOM_VAL = 18;
    public static final byte DEFAULT_ZOOM = 16;

    private int x;
    private int y;
    private int zoom;

    public OsmTileModel() {
    }

    public OsmTileModel(int x, int y, int zoom) {
        this.setX(x);
        this.setY(y);
        this.setZoom(zoom);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        if (zoom >= MIN_ZOOM_VAL && zoom <= MAX_ZOOM_VAL)
            this.zoom = zoom;
        else
            this.zoom = DEFAULT_ZOOM;
    }

    /**
     * Provide api path according to OSM tile filename
     * e.g. /{zoom}/{x}/{y}.png
     * 
     * @return
     */
    public String toApiPathName() {
        return new StringBuilder().append(File.separator).append(zoom)
                .append(File.separator).append(x).append(File.separator)
                .append(y).append(".png").toString();
    }

    /**
     * Provide a filename format to store tile image as
     * e.g. {zoom}_{x}_{y}.png
     * 
     * @return
     */
    public String toFilename() {
        // display as zoom_x_y.png
        return this.toString() + ".png";
    }

    /**
     * Provide a filename format without file type extension to store tile image as
     * e.g. {zoom}_{x}_{y}
     * 
     * @return
     */
    @Override
    public String toString() {
        // displayed as zoom_x_y
        return new StringBuilder().append(zoom).append("_")
                .append(x).append("_").append(y).toString();
    }

}