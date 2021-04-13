package sglib.providers;

import java.util.List;

import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;

public interface IGroundProvider {

	public void setConfiguration(IConfigurationProvider configurationProvider);

	public ListTerm groundSymbol(Literal belief);

	public List<Literal> groundBeliefs(List<Literal> beliefs);

}
