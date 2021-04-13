package sglib.providers;

public interface IGroundedPerceptProvider {

	public void start();

	public void stop();

	public void register(IGroundedPerceptTarget target, String agentName);

	void setConfiguration(IConfigurationProvider configurationProvider);

}
