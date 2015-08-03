package com.deploy.program.deploy.factory;

import com.deploy.program.deploy.interfaces.Deployer;

public abstract class AbstractDeployFactory {

	public abstract Deployer newInstance() ;
}
