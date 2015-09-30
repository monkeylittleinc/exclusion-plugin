package org.jvnet.hudson.plugins.exclusion;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Exclusion resource that can be configured per job.
 *
 * @author Kohsuke Kawaguchi
 * @author Anthony Roux
 */
public class DefaultIdType extends IdType {

    @DataBoundConstructor
    public DefaultIdType(String name) {
        super(name);
    }

    @Override
    public Id allocate(boolean launchAlloc, AbstractBuild<?, ?> build, final IdAllocationManager manager, Launcher launcher, BuildListener buildListener) throws IOException, InterruptedException {
        final String n;

        if (launchAlloc) {
            n = manager.allocate(build, getFixedId(), buildListener);
        } else {
            n = getFixedId();
        }

        return new Id(this) {

            @Override
            public String get() {
                return n;
            }

            @Override
            public void cleanUp() {
                manager.free(n);
            }
        };
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends IdTypeDescriptor {

        @Override
        public String getDisplayName() {
            return "New Resource";
        }
    }
}
