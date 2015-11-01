package com.wayoos.maven.plugin.branch;

/**
 * Created by steph on 01.11.15.
 */
public class BranchManager {

    private final Logger logger;

    public BranchManager(Logger logger) {
        this.logger = logger;
    }

    public void prepare() {
        logger.info("Start branch prepare");
    }

}
