package sglib;

import java.util.logging.Logger;

import jason.JasonException;
import jason.asSemantics.Agent;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;

public class configureGrounders extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	private Logger logger = Logger.getLogger("sglib." + configureGrounders.class.getName());

	@Override
	public int getMinArgs() {
		return 1;
	}

	@Override
	public int getMaxArgs() {
		return 1;
	}

	@Override
	protected void checkArguments(Term[] args) throws JasonException {
		super.checkArguments(args);
		if (!(args[0] instanceof StringTerm))
			throw JasonException.createWrongArgument(this,
					"first argument must be a string with the configuration json");
	}

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
		checkArguments(args);
		logger.info(configureGrounders.class.getName() + " internal action was called.");

		Agent agent = ts.getAg();
		groundSymbol ia = (groundSymbol) agent.getIA(groundSymbol.class.getTypeName());

		if (ia != null) {
			StringTerm json = (StringTerm) args[0];
			ia.configureProviders(json.getString());
			return true;
		}
		return false;
	}

}
