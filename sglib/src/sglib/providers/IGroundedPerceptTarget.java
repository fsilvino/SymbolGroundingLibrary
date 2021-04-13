package sglib.providers;

import java.util.List;

import sglib.percepts.GroundedPercept;

public interface IGroundedPerceptTarget {

	public void addGroundedPercepts(List<GroundedPercept> percepts);

	public void removeGroundedPercepts(List<GroundedPercept> percepts);

}
