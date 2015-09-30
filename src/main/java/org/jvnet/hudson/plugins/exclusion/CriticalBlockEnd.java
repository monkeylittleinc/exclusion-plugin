package org.jvnet.hudson.plugins.exclusion;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Executor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Build step -> End of critical zone
 *
 * @author Anthony Roux
 */
public class CriticalBlockEnd extends Builder {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public CriticalBlockEnd() {
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        final IdAllocationManager pam = IdAllocationManager.getManager(Executor.currentExecutor().getOwner());

        //Get environmental variables
        EnvVars environment = build.getEnvironment(listener);
        List<String> listId = new ArrayList<String>();

        //Add to a list all "variableEnv" (which are added by IdAllocator)
        // Each variableEnv is a resource
        for (Entry<String, String> e : environment.entrySet()) {
            String cle = e.getKey();
            //Only environmental variables from the current job
            String name = "variableEnv" + build.getProject().getName();
            if (cle.contains(name)) {
                String valeur = e.getValue();
                listId.add(valeur);
            }
        }
        if (!listId.isEmpty()) {
            listener.getLogger().println("[Exclusion] -> Releasing all the resources");
        }

        for (String id : listId) {

            DefaultIdType p = new DefaultIdType(id);
            Id i = p.allocate(false, build, pam, launcher, listener);
            AbstractBuild<?, ?> absBuild = IdAllocationManager.getOwnerBuild(i.type.name);
            if (absBuild != null) {
                i.cleanUp();
            }
        }
        return true;
    }

    public String getDisplayName() {
        return "Critical block end";
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return "Critical block end";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/Exclusion/helpCBE.html";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }
    }
}
