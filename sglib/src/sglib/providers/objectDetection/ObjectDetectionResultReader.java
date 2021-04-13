package sglib.providers.objectDetection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import jason.asSyntax.Literal;
import sglib.configuration.SGLibModule;
import sglib.utils.FileUtils;
import sglib.utils.JasonUtils;

public class ObjectDetectionResultReader {

	private Logger logger;
	private DetectionConfiguration configuration;

	public ObjectDetectionResultReader(Logger logger) {
		this.logger = logger;
	}

	public void readConfiguration(SGLibModule module) throws IOException {
		String configFilePath = (String) module
				.getConfiguration(SGLibObjectDetectionModule.OBJECT_DETECTION_MODULE_CONFIG_FILE_LOCATION_KEY, "");
		this.configuration = new DetectionConfiguration(configFilePath);
	}

	public Map<String, Literal> readDetectionResult() {
		Map<String, Literal> detections = new HashMap<String, Literal>();

		try {
			String detectionResultFile = configuration.getDetectionResultFilePath();
			logger.info("Reading from: " + detectionResultFile);
			String json = FileUtils.readFileContents(detectionResultFile);
			JSONArray jsonDetections = new JSONArray(json);

			for (int i = 0; i < jsonDetections.length(); i++) {
				JSONObject jsonObject = jsonDetections.getJSONObject(i);
				String percept = jsonObject.getString("percept");
				float score = jsonObject.getFloat("score");
				if (score >= 0.5) {
					JSONObject jsonObjectClass = jsonObject.getJSONObject("class");

					String className = jsonObjectClass.getString("name");
					Literal litPercept = Literal
							.parseLiteral(percept + "(" + convertClassToValidJasonValue(className) + ")");

					if (jsonObject.has("annots")) {
						JSONArray annots = jsonObject.getJSONArray("annots");
						for (int j = 0; j < annots.length(); j++) {
							Literal annot = Literal.parseLiteral(annots.getString(j));
							litPercept.addAnnot(annot);
						}
					}

					logger.info(litPercept.toString() + " was read from json file.");
					detections.put(JasonUtils.generateLiteralId(litPercept), litPercept);
				}
			}

			return detections;
		} catch (IOException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private static String convertClassToValidJasonValue(String originalClassName) {
		String result = originalClassName.replaceAll("[\\s,]", "_").replaceAll("[\"'()]", "");
		result = result.substring(0, 1).toLowerCase() + result.substring(1);
		return result;
	}

	private class DetectionConfiguration {

		private String resultBasePath;

		public DetectionConfiguration(String configFilePath) throws IOException {
			String json = FileUtils.readFileContents(configFilePath);
			JSONObject jsonObj = new JSONObject(json);

			setResultBasePath(jsonObj.getString("RESULT_BASE_PATH"));
		}

		public String getResultBasePath() {
			return resultBasePath;
		}

		public void setResultBasePath(String resultBasePath) {
			this.resultBasePath = resultBasePath;
		}

		public String getDetectionResultFilePath() {
			return this.getResultBasePath() + "\\agent.perception.json";
		}

	}

}