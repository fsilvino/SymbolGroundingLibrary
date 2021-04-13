package sglib.environments;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jason.environment.Environment;
import sglib.configuration.SGLibConfiguration;
import sglib.configuration.SGLibModule;
import sglib.percepts.GroundedPercept;
import sglib.providers.IConfigurationProvider;
import sglib.providers.IGroundedPerceptProvider;
import sglib.providers.IGroundedPerceptTarget;

public abstract class BaseGroundedPerceptTargetEnvironment extends Environment
		implements IGroundedPerceptTarget, IConfigurationProvider {

	protected SGLibConfiguration configuration;
	protected Logger logger = Logger.getLogger("sgmodel_v2." + this.getClass().getName());
	protected ArrayList<IGroundedPerceptProvider> providers = new ArrayList<>();

	/** Called before the MAS execution with the args informed in .mas2j */
	@Override
	public void init(String[] args) {
		super.init(args);
		this.configuration = this.createSGLibConfiguration();
		this.loadProvidersAndRegister();
		this.setProvidersConfiguration();
		this.startProviders();
	}

	private void setProvidersConfiguration() {
		for (IGroundedPerceptProvider provider : providers) {
			provider.setConfiguration(this);
		}
	}

	protected abstract SGLibConfiguration createSGLibConfiguration();

	protected abstract void loadProvidersAndRegister();

	protected void startProviders() {
		for (IGroundedPerceptProvider provider : this.providers) {
			provider.start();
		}
	}

	protected void stopProviders() {
		for (IGroundedPerceptProvider provider : this.providers) {
			provider.stop();
		}
	}

	@Override
	public SGLibModule getModule(String name) {
		return this.configuration.getModule(name);
	}

	/** Called before the end of MAS execution */
	@Override
	public void stop() {
		this.stopProviders();
		super.stop();
	}

	@Override
	public void addGroundedPercepts(List<GroundedPercept> percepts) {
		logger.info("Environment received " + percepts.size() + " percepts from provider to be added");
		for (GroundedPercept percept : percepts) {
			if (percept.forSpecificAgent())
				addPercept(percept.getAgentName(), percept.getPercept());
			else
				addPercept(percept.getPercept());
		}
	}

	@Override
	public void removeGroundedPercepts(List<GroundedPercept> percepts) {
		logger.info("Environment received " + percepts.size() + " percepts from provider to be removed");
		for (GroundedPercept percept : percepts) {
			logger.info("Environment removing percept: " + percept.getPercept().toString());
			if (percept.forSpecificAgent())
				removePercept(percept.getAgentName(), percept.getPercept());
			else
				removePercept(percept.getPercept());
		}
	}

}
