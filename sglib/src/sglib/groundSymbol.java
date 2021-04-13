package sglib;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Logger;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import sglib.configuration.SGLibConfiguration;
import sglib.configuration.SGLibModule;
import sglib.providers.IConfigurationProvider;
import sglib.providers.IGroundProvider;

public class groundSymbol extends DefaultInternalAction implements IConfigurationProvider {

	private static final long serialVersionUID = 1L;

	private Logger logger = Logger.getLogger("sglib." + groundSymbol.class.getName());
	private HashMap<String, IGroundProvider> groundProviders = new HashMap<String, IGroundProvider>();
	private SGLibConfiguration configuration;

	public groundSymbol() {
	}

	@Override
	public int getMinArgs() {
		return 2;
	}

	@Override
	public int getMaxArgs() {
		return 2;
	}

	@Override
	protected void checkArguments(Term[] args) throws JasonException {
		super.checkArguments(args);
		if (!(args[0] instanceof Literal))
			throw JasonException.createWrongArgument(this, "first argument must be a literal");
	}

	public void configureProviders(String configurationJson) {
		this.configuration = new SGLibConfiguration(configurationJson);
	}

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
		checkArguments(args);

		logger.info(groundSymbol.class.getName() + " internal action was called.");
		if (this.configuration == null)
			throw new JasonException(
					"You must configure ground providers before using the groundSymbol internal action.");

		ListTerm result = new ListTermImpl();
		Literal belief = (Literal) args[0];

		for (String providerName : configuration.getProviders()) {
			if (!groundProviders.containsKey(providerName)) {
				try {
					Class<?> clazz = Class.forName(providerName);
					Constructor<?> constructor = clazz.getConstructor();
					IGroundProvider providerInstance = (IGroundProvider) constructor.newInstance();
					providerInstance.setConfiguration(this);
					groundProviders.put(providerName, providerInstance);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}

			IGroundProvider providerInstance = groundProviders.get(providerName);
			if (providerInstance == null)
				continue;

			ListTerm providerResult = providerInstance.groundSymbol(belief);

			if (providerResult != null)
				result.addAll(providerResult);
		}

		logger.info(result.size() + " grounds found to belief " + belief.toString());
		logger.info(result.toString());

		return un.unifies(args[1], result);
	}

	@Override
	public SGLibModule getModule(String name) {
		return configuration.getModule(name);
	}
}
