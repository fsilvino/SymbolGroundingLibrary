package sglib.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import sglib.providers.objectDetection.ObjectDetectionGroundProvider;

public class SGLibConfiguration {

	public final static String PROVIDERS_CONFIGURATION_KEY = "providers";
	public final static String MODULES_CONFIGURATION_KEY = "modules";

	private ArrayList<String> providers = new ArrayList<>();
	private Map<String, SGLibModule> modules = new HashMap<>();
	private Logger logger = Logger.getLogger("sglib." + ObjectDetectionGroundProvider.class.getName());

	public SGLibConfiguration(String json) {
		JSONObject jsonObj = new JSONObject(json);

		try {
			if (jsonObj.has(PROVIDERS_CONFIGURATION_KEY)) {
				JSONArray jsonProviders = jsonObj.getJSONArray(PROVIDERS_CONFIGURATION_KEY);
				for (int i = 0; i < jsonProviders.length(); i++) {
					providers.add(jsonProviders.getString(i));
				}
			}
		} catch (Exception e) {
			logger.severe("Error reading providers: " + e.getMessage());
			throw e;
		}

		try {
			if (jsonObj.has(MODULES_CONFIGURATION_KEY)) {
				JSONArray jsonModules = jsonObj.getJSONArray(MODULES_CONFIGURATION_KEY);
				for (int i = 0; i < jsonModules.length(); i++) {
					JSONObject jsonModule = jsonModules.getJSONObject(i);
					SGLibModule module = new SGLibModule();
					module.setName(jsonModule.getString(SGLibModule.MODULE_NAME_KEY));

					for (String key : jsonModule.keySet()) {
						if (!key.equals(SGLibModule.MODULE_NAME_KEY)) {
							module.setConfiguration(key, jsonModule.get(key));
						}
					}

					modules.put(module.getName(), module);
				}
			}
		} catch (Exception e) {
			logger.severe("Error reading modules: " + e.getMessage());
			throw e;
		}
	}

	public ArrayList<String> getProviders() {
		return this.providers;
	}

	public SGLibModule getModule(String name) {
		return modules.get(name);
	}
}