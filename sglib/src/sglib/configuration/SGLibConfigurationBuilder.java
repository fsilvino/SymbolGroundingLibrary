package sglib.configuration;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class SGLibConfigurationBuilder {

	public ArrayList<SGLibModule> modules = new ArrayList<>();
	public ArrayList<String> providers = new ArrayList<>();

	public SGLibConfigurationBuilder() {

	}

	public SGLibConfigurationBuilder withProvider(String providerName) {
		providers.add(providerName);
		return this;
	}

	public SGLibConfigurationBuilder withModule(SGLibModule module) {
		modules.add(module);
		return this;
	}

	public String toJson() {
		JSONObject obj = new JSONObject();

		JSONArray arrModules = new JSONArray();
		for (SGLibModule module : this.modules) {
			JSONObject objModule = new JSONObject();
			objModule.put(SGLibModule.MODULE_NAME_KEY, module.getName());
			Map<String, Object> configurations = module.getConfigurations();
			for (String configKey : configurations.keySet()) {
				objModule.put(configKey, configurations.get(configKey));
			}
			arrModules.put(objModule);
		}

		obj.put(SGLibConfiguration.PROVIDERS_CONFIGURATION_KEY, new JSONArray(this.providers));
		obj.put(SGLibConfiguration.MODULES_CONFIGURATION_KEY, arrModules);

		return obj.toString();
	}

	public SGLibConfiguration build() {
		return new SGLibConfiguration(this.toJson());
	}

}
