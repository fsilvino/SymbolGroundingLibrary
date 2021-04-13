package sglib.providers;

import sglib.configuration.SGLibModule;

public interface IConfigurationProvider {

	public SGLibModule getModule(String name);

}
