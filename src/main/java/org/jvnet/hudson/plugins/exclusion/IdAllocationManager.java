package org.jvnet.hudson.plugins.exclusion;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Kohsuke Kawaguchi
 * @author Anthony Roux
 */
public final class IdAllocationManager {

    private final static Map<String, AbstractBuild<?, ?>> ids = new HashMap<String, AbstractBuild<?, ?>>();
    private static final Map<Computer, WeakReference<IdAllocationManager>> INSTANCES = new WeakHashMap<Computer, WeakReference<IdAllocationManager>>();
    private final Computer node; // TODO unused

    private IdAllocationManager(Computer node) {
        this.node = node;
    }

    public static IdAllocationManager getManager(Computer node) {
        IdAllocationManager pam;
        WeakReference<IdAllocationManager> ref = INSTANCES.get(node);
        if (ref != null) {
            pam = ref.get();
            if (pam != null) {
                return pam;
            }
        }
        pam = new IdAllocationManager(node);
        INSTANCES.put(node, new WeakReference<IdAllocationManager>(pam));
        return pam;
    }

    /*package*/
    static AbstractBuild<?, ?> getOwnerBuild(String resource) {
        return ids.get(resource);
    }

    /*package*/
    static HashMap<String, AbstractBuild<?, ?>> getAllocations() {
        return new HashMap<String, AbstractBuild<?, ?>>(ids);
    }

    public synchronized String allocate(AbstractBuild<?, ?> owner, String id, BuildListener buildListener) throws InterruptedException, IOException {
        PrintStream logger = buildListener.getLogger();
        boolean printed = false;

        while (ids.get(id) != null) {

            if (printed == false) {
                logger.printf("[Exclusion] -> Waiting for resource '%s' currently used by '%s'%n", id, ids.get(id).toString());
                printed = true;
            }
            wait(1000);
        }

        // When allocate a resource, add it to the hashmap
        ids.put(id, owner);
        return id;
    }

    /**
     * Release a resource
     */
    public synchronized void free(String n) {
        ids.remove(n);
        notifyAll();
    }
}
