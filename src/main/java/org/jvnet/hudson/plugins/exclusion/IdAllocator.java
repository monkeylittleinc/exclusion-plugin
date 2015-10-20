package org.jvnet.hudson.plugins.exclusion;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
import hudson.tasks.BuildWrapper;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 * @author Anthony Roux
 */
public class IdAllocator extends BuildWrapper {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    private static List<RessourcesMonitor> listRessources = new ArrayList<RessourcesMonitor>();
    private static String jName = "unknow";
    //Resources currently configured in the job
    private IdType[] ids = null;

    public IdAllocator(IdType[] ids) {
        this.ids = ids;
    }

    public static List<RessourcesMonitor> getListRessources() {
        return listRessources;
    }

    /**
     * This method update Job name
     *
     * @param oldBuildNumber  : Old build number
     * @param newBuildNumber : New build number
     */
    private static void updateList(Integer oldBuildNumber, Integer newBuildNumber) {
        for (int i = listRessources.size() - 1; i >= 0; i--) {
            if (listRessources.get(i).getBuildNumber().equals(oldBuildNumber)) {
                String ressource = listRessources.get(i).getRessource();
                listRessources.remove(i);
                listRessources.add(new RessourcesMonitor(newBuildNumber, ressource));
            }
        }
    }

    /**
     * This method removes all the resources of a build
     *
     * @param buildNumber : build number
     */
    /*package*/
    static void deleteList(String buildNumber) {
        for (int i = listRessources.size() - 1; i >= 0; i--) {
            if (listRessources.get(i).getBuildNumber().equals(buildNumber)) {
                listRessources.remove(i);
            }
        }
    }

    /**
     * This method changes the state of a resource (in use or not)
     *
     * @param buildNumber  : build number
     * @param resourceName : Resource name
     * @param build        : resource state (true = in use)
     */
    /*package*/
    static void updateBuild(Integer buildNumber, String resourceName, boolean build) {
        for (int i = listRessources.size() - 1; i >= 0; i--) {
            if (listRessources.get(i).getBuildNumber().equals(buildNumber) && listRessources.get(i).getRessource().equals(resourceName)) {
                RessourcesMonitor rmGet = listRessources.get(i);
                listRessources.remove(i);
                rmGet.setBuild(build);
                listRessources.add(rmGet);
            }
        }
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        final List<String> allocated = new ArrayList<String>();
        final List<Id> alloc = new ArrayList<Id>();
        final Integer buildNumber = build.getNumber();
        final Computer cur = Executor.currentExecutor().getOwner();
        final IdAllocationManager pam = IdAllocationManager.getManager(cur);
        for (IdType pt : ids) {
            allocated.add(pt.name);
            Id p = pt.allocate(false, build.getNumber(), pam, launcher, listener);
            alloc.add(p);
        }

        return new Environment() {

            @Override
            public boolean tearDown(AbstractBuild abstractBuild, BuildListener buildListener) throws IOException, InterruptedException {
                PrintStream logger = buildListener.getLogger();
                logger.println("[Exclusion] -> tear down");
                for(Id p : alloc) {
                    Integer get = IdAllocationManager.getOwnerBuild(p.type.name);
                    if(get != null) {
                        logger.println(get);
                        logger.println(abstractBuild.getNumber());
                        if(get.equals(abstractBuild.getNumber())) {
                            logger.println("[Exclusion] -> Releasing " + p.type.name);
                            p.cleanUp();
                        } else {
                            logger.println("[Exclusion] -> Not releasing " + p.type.name);
                        }
                    }
                }
                return true;
            }

            @Override
            public void buildEnvVars(Map<String, String> env) {
                int i = 0;
                for (String p : allocated) {
                    env.put("variableEnv" + buildNumber + i, p);
                    env.put(p, p);
                    i++;
                }
            }
        };
    }

    public IdType[] getIds() {
        return ids;
    }

    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        @Override
        public String getDisplayName() {
            return "Add resource to manage exclusion";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/Exclusion/help.html";
        }

        public List<IdTypeDescriptor> getIdTypes() {
            return IdTypeDescriptor.all();
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            List<IdType> ids = Descriptor.newInstancesFromHeteroList(
                    req, formData, "ids", IdTypeDescriptor.all());
            // In some cases you can not get the job name as previously, so we let Newinstance do it
            String[] split = req.getReferer().split("/");
            for (int i = 0; i < split.length; i++) {
                if (split[i].equals("job")) {
                    setName(split[i + 1]);
                }
            }
            IdAllocator portAlloc = new IdAllocator(ids.toArray(new IdType[ids.size()]));
            return portAlloc;
        }

        // TODO introduced to keep things working in unittest too. jName has to die as soon as we have decent coverage.
        /*package*/ void setName(String name) {
            jName = name;
        }
    }
}
