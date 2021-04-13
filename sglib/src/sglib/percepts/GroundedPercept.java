package sglib.percepts;

import jason.asSyntax.Literal;

public class GroundedPercept {

	private Literal percept;
	private String agentName;

	public GroundedPercept(String agentName, Literal percept) {
		this.agentName = agentName;
		this.percept = percept;
	}

	public Literal getPercept() {
		return percept;
	}

	public String getAgentName() {
		return agentName;
	}

	public boolean forSpecificAgent() {
		return this.agentName != null && !this.agentName.isEmpty();
	}

}
