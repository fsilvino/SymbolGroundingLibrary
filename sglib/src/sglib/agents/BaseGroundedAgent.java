/**
 * 
 */
package sglib.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import jason.asSemantics.Agent;
import jason.asSyntax.Literal;
import sglib.configuration.SGLibConfiguration;
import sglib.configuration.SGLibModule;
import sglib.providers.IConfigurationProvider;
import sglib.providers.IGroundProvider;

public abstract class BaseGroundedAgent extends Agent implements IConfigurationProvider {

	private static final long serialVersionUID = 1L;

	private Logger logger = Logger.getLogger("sglib.agents." + getClass().getName());
	protected ArrayList<IGroundProvider> groundProviders;
	private SGLibConfiguration configuration;

	public BaseGroundedAgent() {
		super();
		this.configuration = this.createSGLibConfiguration();
		this.loadGroundProviders();
		this.setProvidersConfiguration();
	}

	private void setProvidersConfiguration() {
		for (IGroundProvider provider : groundProviders) {
			provider.setConfiguration(this);
		}
	}

	@Override
	public SGLibModule getModule(String name) {
		return this.configuration.getModule(name);
	}

	protected abstract SGLibConfiguration createSGLibConfiguration();

	protected abstract ArrayList<IGroundProvider> createGroundProviders();

	private void loadGroundProviders() {
		this.groundProviders = this.createGroundProviders();
		if (this.groundProviders == null) {
			this.groundProviders = new ArrayList<IGroundProvider>();
		}
	}

	protected List<Literal> ground(List<Literal> percepts) {
		logger.info("ground() called. " + percepts.size() + " percepts received. Calling providers...");
		for (IGroundProvider provider : this.groundProviders) {
			percepts = provider.groundBeliefs(percepts);
		}
		return percepts;
	}

	@Override
	public int buf(Collection<Literal> percepts) {
		if (percepts != null && !percepts.isEmpty()) {
			percepts = this.ground(new ArrayList<Literal>(percepts));
		}

		return super.buf(percepts);
	}

	@Override
	public void initAg() {
		// TODO Auto-generated method stub
		super.initAg();
	}

	@Override
	public void stopAg() {
		// TODO Auto-generated method stub
		super.stopAg();
	}

}
