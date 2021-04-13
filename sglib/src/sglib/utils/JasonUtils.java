package sglib.utils;

import jason.asSyntax.Literal;
import jason.asSyntax.Term;

public class JasonUtils {

	private JasonUtils() {
	}

	public static String generateLiteralId(Literal percept) {
		String id = percept.getFunctor();

		String separator = "";
		for (Term term : percept.getTerms()) {
			id += separator + term.toString();
			separator = ",";
		}

		return id;
	}

}
