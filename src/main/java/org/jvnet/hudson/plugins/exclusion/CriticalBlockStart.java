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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Build step -> Start of critical zone
 *
 * @author Anthony Roux
 */
public class CriticalBlockStart extends Builder {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static IdAllocationManager pam = null;

    @DataBoundConstructor
    public CriticalBlockStart() {
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        pam = IdAllocationManager.getManager(Executor.currentExecutor().getOwner());

        PrintStream logger = listener.getLogger();

        EnvVars environment = build.getEnvironment(listener);
        final List<String> listId = new ArrayList<String>();
        // Add to a list all "variableEnv" (which are added by IdAllocator)
        // Each variableEnv is a resource
        for (Entry<String, String> e : environment.entrySet()) {
            String cle = e.getKey();

            String name = "variableEnv" + build.getNumber();
            if (cle.contains(name)) {
                String valeur = e.getValue();
                listId.add(valeur);
            }
        }

        for (String id : listId) {
            DefaultIdType p = new DefaultIdType(id);

            logger.println("[Exclusion] -> Allocating resource : " + id);
            //Allocating resources
            // if one is already used, just wait for it to be released
            Id resource = p.allocate(true, build.getNumber(), pam, launcher, listener);

            logger.println("[Exclusion] -> Assigned " + resource.get());
        }
        if (!listId.isEmpty()) {
            logger.println("[Exclusion] -> Resource allocation complete");
        }
        return true;
    }

    public String getDisplayName() {
        return "Critical block start";
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return "Critical block start";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/Exclusion/helpCBS.html";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }
    }
}
