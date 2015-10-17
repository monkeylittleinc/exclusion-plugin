package org.jvnet.hudson.plugins.exclusion;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Anthony Roux
 */
public class RessourcesMonitor implements Cloneable {

    private String ressource;
    private Integer buildNumber;
    private boolean build = false;

    public RessourcesMonitor(Integer buildNumber, String ressource) {
        this(buildNumber, ressource, false);
    }

    public RessourcesMonitor(Integer buildNumber, String ressource, boolean build) {
        this.ressource = ressource;
        this.buildNumber = buildNumber;
        this.build = build;
    }

    @Override
    public RessourcesMonitor clone() {
        RessourcesMonitor rm = null;
        try {
            rm = (RessourcesMonitor) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(RessourcesMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rm;
    }

    public boolean getBuild() {
        return build;
    }

    public void setBuild(boolean build) {
        this.build = build;
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public String getRessource() {
        return ressource;
    }
}
