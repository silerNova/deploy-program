package com.deploy.program.deploy.factory.extend;

import com.deploy.program.deploy.factory.AbstractDeployFactory;
import com.deploy.program.deploy.impl.WarDeployer;
import com.deploy.program.deploy.interfaces.Deployer;

public class WarDeployFactory extends AbstractDeployFactory {

	@Override
	public Deployer newInstance() {
		return new WarDeployer();
	}

}
