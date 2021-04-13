package sglib.providers.objectDetection;

import sglib.configuration.SGLibModule;

public class SGLibObjectDetectionModule extends SGLibModule {

	public final static String OBJECT_DETECTION_MODULE_NAME = "objectDetection";
	public final static String OBJECT_DETECTION_MODULE_HOST_CONFIGURATION_KEY = "host";
	public final static String OBJECT_DETECTION_MODULE_PORT_CONFIGURATION_KEY = "serverPort";
	public final static String OBJECT_DETECTION_MODULE_LISTENER_PORT_CONFIGURATION_KEY = "listenerPort";
	public final static String OBJECT_DETECTION_MODULE_CONFIG_FILE_LOCATION_KEY = "configFilePath";

	public SGLibObjectDetectionModule() {
		setName(OBJECT_DETECTION_MODULE_NAME);
	}

	public void setHost(String host) {
		setConfiguration(OBJECT_DETECTION_MODULE_HOST_CONFIGURATION_KEY, host);
	}

	public void setServerPort(int port) {
		setConfiguration(OBJECT_DETECTION_MODULE_PORT_CONFIGURATION_KEY, port);
	}

	public void setListenerPort(int port) {
		setConfiguration(OBJECT_DETECTION_MODULE_LISTENER_PORT_CONFIGURATION_KEY, port);
	}

	public void setConfigFilePath(String configFilePath) {
		setConfiguration(OBJECT_DETECTION_MODULE_CONFIG_FILE_LOCATION_KEY, configFilePath);
	}
}
