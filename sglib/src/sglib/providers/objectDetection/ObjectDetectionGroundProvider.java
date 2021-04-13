package sglib.providers.objectDetection;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import sglib.configuration.SGLibModule;
import sglib.providers.IConfigurationProvider;
import sglib.providers.IGroundProvider;
import sglib.utils.JasonUtils;

public class ObjectDetectionGroundProvider implements IGroundProvider {

	private ObjectDetectionResultReader reader;
	private ObjectDetectionExecutor executor;
	private Logger logger = Logger.getLogger("sglib." + ObjectDetectionGroundProvider.class.getName());

	public ObjectDetectionGroundProvider() {
		this.reader = new ObjectDetectionResultReader(logger);
		this.executor = new ObjectDetectionExecutor();
	}

	private Map<String, Literal> runDetection() throws UnknownHostException, IOException {
		Map<String, Literal> detections = null;

		logger.info("runDetection called");

		DetectionExecutionResult result = this.executor.execute(logger);
		if (result.getResultCode() == DetectionExecutionResultCode.OK
				|| result.getResultCode() == DetectionExecutionResultCode.Unchanged) {
			logger.info("Reading result file...");
			detections = reader.readDetectionResult();
		} else {
			logger.info("Result: " + result.getMessage());
		}

		logger.info("end of runDetection");

		return detections;
	}

	public ListTerm groundSymbol(Literal belief) {
		ListTerm result = new ListTermImpl();

		try {
			Map<String, Literal> detections = runDetection();
			if (detections != null && !detections.isEmpty()) {
				logger.info("detections is not empty");
				String beliefId = JasonUtils.generateLiteralId(belief);
				Literal detection = detections.get(beliefId);
				if (detection != null) {
					result.add(Literal.parseLiteral("visual"));
				}
			} else {
				logger.info("detections is null or empty");
			}
		} catch (UnknownHostException e) {
			logger.severe(e.getMessage());
		} catch (IOException e) {
			logger.severe(e.getMessage());
		}

		return result;
	}

	@Override
	public void setConfiguration(IConfigurationProvider configurationProvider) {
		SGLibModule module = configurationProvider.getModule(SGLibObjectDetectionModule.OBJECT_DETECTION_MODULE_NAME);
		if (module != null) {
			this.executor.configure(module);
			try {
				this.reader.readConfiguration(module);
			} catch (IOException e) {
				logger.severe("The configuration file could not be loaded");
				e.printStackTrace();
			}
		}
	}

	@Override
	public List<Literal> groundBeliefs(List<Literal> beliefs) {
		List<Literal> result = new ArrayList<Literal>();

		try {
			Map<String, Literal> detections = runDetection();
			if (detections != null && !detections.isEmpty()) {
				logger.info("detections is not empty");

				for (Literal belief : beliefs) {
					String beliefId = JasonUtils.generateLiteralId(belief);
					Literal detection = detections.get(beliefId);
					if (detection != null) {
						belief.addAnnot(Literal.parseLiteral("grounded"));
						belief.addAnnot(Literal.parseLiteral("visual"));
					}
					result.add(belief);
				}

			} else {
				logger.info("detections is null or empty");
			}
		} catch (UnknownHostException e) {
			logger.severe(e.getMessage());
		} catch (IOException e) {
			logger.severe(e.getMessage());
		}

		return result;
	}

}