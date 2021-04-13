package sglib.configuration;

import java.util.HashMap;
import java.util.Map;

public class SGLibModule {

	public static final String MODULE_NAME_KEY = "name";

	private String name;
	private HashMap<String, Object> configurations;

	public SGLibModule() {
		this.configurations = new HashMap<String, Object>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Object> getConfigurations() {
		return this.configurations;
	}

	public Object getConfiguration(String key, Object defaultValue) {
		return configurations.getOrDefault(key, defaultValue);
	}

	public void setConfiguration(String key, Object value) {
		this.configurations.put(key, value);
	}

}
