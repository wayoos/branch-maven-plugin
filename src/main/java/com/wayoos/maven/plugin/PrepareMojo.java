package com.wayoos.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.wayoos.maven.plugin.branch.BranchManager;
import com.wayoos.maven.plugin.branch.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.*;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;

import java.io.File;

/**
 * Goal which touches a timestamp file.
 *
 */
@Mojo( name = "prepare", defaultPhase = LifecyclePhase.VALIDATE )
public class PrepareMojo
    extends AbstractMojo
{

    /** A full name of the versions-maven-plugin set goal. */
    private static final String VERSIONS_MAVEN_PLUGIN_SET_GOAL = "org.codehaus.mojo:versions-maven-plugin:2.2:set";

    /** Success exit code. */
    private static final int SUCCESS_EXIT_CODE = 0;

    /** Command line for Maven executable. */
    private final Commandline cmdMvn = new Commandline();

    /**
     * The path to the Maven executable. Defaults to either "mvn" or "mvn.bat"
     * depending on the operating system.
     */
    @Parameter(property = "mvnExecutable")
    private String mvnExecutable;

    /** Whether to print commands output into the console. */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose = false;

    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.basedir}", property = "basedir", required = true )
    private File basedir;


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        BranchManager branchManager = new BranchManager(new Logger() {
            public void info(String message) {
                getLog().info(message);
            }
        });

        branchManager.prepare();

        try {
            mvnSetVersions("2-SNAPSHOT");
        } catch (CommandLineException e) {
            getLog().error(e);
        }
    }

    /**
     * Initializes command line executables.
     *
     */
    private void initExecutables() {
        if (StringUtils.isBlank(cmdMvn.getExecutable())) {
            if (StringUtils.isBlank(mvnExecutable)) {
                mvnExecutable = "mvn"
                        + (Os.isFamily(Os.FAMILY_WINDOWS) ? ".bat" : "");
            }
            cmdMvn.setExecutable(mvnExecutable);
        }
    }

    /**
     * Executes 'set' goal of versions-maven-plugin or 'set-version' of
     * tycho-versions-plugin in case it is tycho build.
     *
     * @param version
     *            New version to set.
     * @throws MojoFailureException
     * @throws CommandLineException
     */
    protected void mvnSetVersions(final String version)
            throws MojoFailureException, CommandLineException {
        getLog().info("Updating version(s) to '" + version + "'.");

        executeMvnCommand(VERSIONS_MAVEN_PLUGIN_SET_GOAL, "-DnewVersion="
                    + version, "-DgenerateBackupPoms=false");
    }

    /**
     * Executes Maven command.
     *
     * @param args
     *            Maven command line arguments.
     * @throws CommandLineException
     * @throws MojoFailureException
     */
    private void executeMvnCommand(final String... args)
            throws CommandLineException, MojoFailureException {
        executeCommand(cmdMvn, true, args);
    }

    /**
     * Executes command line.
     *
     * @param cmd
     *            Command line.
     * @param failOnError
     *            Whether to throw exception on NOT success exit code.
     * @param args
     *            Command line arguments.
     * @return {@link CommandResult} instance holding command exit code, output
     *         and error if any.
     * @throws CommandLineException
     * @throws MojoFailureException
     *             If <code>failOnError</code> is <code>true</code> and command
     *             exit code is NOT equals to 0.
     */
    private CommandResult executeCommand(final Commandline cmd,
                                         final boolean failOnError, final String... args)
            throws CommandLineException, MojoFailureException {
        // initialize executables
        initExecutables();

        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    cmd.getExecutable() + " " + StringUtils.join(args, " "));
        }

        cmd.clearArgs();
        cmd.addArguments(args);

        final StreamConsumer out;
        if (verbose) {
            out = new DefaultConsumer();
        } else {
            out = new CommandLineUtils.StringStreamConsumer();
        }

        final CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

        // execute
        final int exitCode = CommandLineUtils.executeCommandLine(cmd, out, err);

        String errorStr = err.getOutput();
        String outStr = "";
        if (out instanceof StringStreamConsumer) {
            outStr = ((StringStreamConsumer) out).getOutput();
        }

        if (failOnError && exitCode != SUCCESS_EXIT_CODE) {
            // not all commands print errors to error stream
            if (StringUtils.isBlank(errorStr) && StringUtils.isNotBlank(outStr)) {
                errorStr = outStr;
            }

            throw new MojoFailureException(errorStr);
        }

        return new CommandResult(exitCode, outStr, errorStr);
    }

    private static class CommandResult {
        private final int exitCode;
        private final String out;
        private final String error;

        private CommandResult(final int exitCode, final String out,
                              final String error) {
            this.exitCode = exitCode;
            this.out = out;
            this.error = error;
        }

        /**
         * @return the exitCode
         */
        public int getExitCode() {
            return exitCode;
        }

        /**
         * @return the out
         */
        public String getOut() {
            return out;
        }

        /**
         * @return the error
         */
        public String getError() {
            return error;
        }
    }
}
