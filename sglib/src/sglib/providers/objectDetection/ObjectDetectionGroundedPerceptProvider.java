package sglib.providers.objectDetection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import jason.asSyntax.Literal;
import sglib.configuration.SGLibModule;
import sglib.percepts.GroundedPercept;
import sglib.providers.IConfigurationProvider;
import sglib.providers.IGroundedPerceptProvider;
import sglib.providers.IGroundedPerceptTarget;
import sglib.utils.JasonUtils;

public class ObjectDetectionGroundedPerceptProvider implements IGroundedPerceptProvider {

	private ObjectDetectionResultReader reader;
	private static int runDetectionIntervalInSeconds = 10;
	private Timer timer = new Timer();
	private Map<String, Literal> detections = new HashMap<String, Literal>();
	private ArrayList<RegisteredTarget> targets = new ArrayList<>();
	private Logger logger = Logger.getLogger("sglib." + ObjectDetectionGroundedPerceptProvider.class.getName());
	private ObjectDetectionExecutor executor;

	public ObjectDetectionGroundedPerceptProvider() {
		this.reader = new ObjectDetectionResultReader(logger);
		this.executor = new ObjectDetectionExecutor();
	}

	@Override
	public void register(IGroundedPerceptTarget target, String agentName) {
		targets.add(new RegisteredTarget(target, agentName));
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

	private void updateDetections(Map<String, Literal> newDetections) {
		if (newDetections.isEmpty()) {
			logger.info("New detections is empty. Exiting from update detections...");
			return;
		}

		ArrayList<Literal> toRemove = new ArrayList<>();
		for (Literal detection : this.detections.values()) {
			String detectionId = JasonUtils.generateLiteralId(detection);
			if (!newDetections.containsKey(detectionId)) {
				logger.info("Removing detection: " + detection.toString());
				toRemove.add(detection);
			}
		}

		removePerceptsFromTargets(toRemove);

		for (Literal removedDetection : toRemove) {
			this.detections.remove(JasonUtils.generateLiteralId(removedDetection));
		}

		ArrayList<Literal> toAdd = new ArrayList<>();
		for (Literal newDetection : newDetections.values()) {
			newDetection.addAnnot(Literal.parseLiteral("grounded"));
			newDetection.addAnnot(Literal.parseLiteral("visual"));
			String detectionId = JasonUtils.generateLiteralId(newDetection);
			if (!this.detections.containsKey(detectionId)) {
				this.detections.put(detectionId, newDetection);
				logger.info("Adding detection: " + newDetection.toString());
				toAdd.add(newDetection);
			}
		}

		addPerceptsToTargets(toAdd);
	}

	private void addPerceptsToTargets(ArrayList<Literal> toAdd) {
		if (toAdd.isEmpty())
			return;

		logger.info("Adding " + toAdd.size() + " percepts to targets");
		for (RegisteredTarget target : this.targets) {
			target.getTarget().addGroundedPercepts(convertToGroundedPerceptList(target.getAgentName(), toAdd));
		}
	}

	private void removePerceptsFromTargets(ArrayList<Literal> toRemove) {
		if (toRemove.isEmpty())
			return;

		logger.info("Removing " + toRemove.size() + " percepts from targets");
		for (RegisteredTarget target : this.targets) {
			target.getTarget().removeGroundedPercepts(convertToGroundedPerceptList(target.getAgentName(), toRemove));
		}
	}

	private static ArrayList<GroundedPercept> convertToGroundedPerceptList(String agentName, ArrayList<Literal> list) {
		ArrayList<GroundedPercept> groundedList = new ArrayList<>();
		for (Literal percept : list) {
			groundedList.add(new GroundedPercept(agentName, percept));
		}
		return groundedList;
	}

	private void executeDetectionUpdate() {
		DetectionExecutionResult result = executor.execute(logger);
		if (result.getResultCode() == DetectionExecutionResultCode.OK
				|| result.getResultCode() == DetectionExecutionResultCode.Unchanged) {
			logger.info("detection returned ok. Reading result file...");
			Map<String, Literal> detections = reader.readDetectionResult();
			if (detections != null) {
				updateDetections(detections);
			} else {
				logger.info("Read detection result returned null");
			}
		}
		scheduleNextDetectionExecution();
	}

	private void scheduleNextDetectionExecution() {
		this.timer.schedule(new CheckObjectsTimerTask(), runDetectionIntervalInSeconds * 1000);
	}

	@Override
	public void start() {
		logger.info("Provider started.");
		executeDetectionUpdate();
	}

	@Override
	public void stop() {
		if (this.timer != null) {
			this.timer.cancel();
		}
	}

	private class CheckObjectsTimerTask extends TimerTask {

		@Override
		public void run() {
			logger.info("Running detection from timer task...");
			executeDetectionUpdate();
		}

	}

	private class RegisteredTarget {

		private IGroundedPerceptTarget target;
		private String agentName;

		public RegisteredTarget(IGroundedPerceptTarget target, String agentName) {
			this.target = target;
			this.agentName = agentName;
		}

		public IGroundedPerceptTarget getTarget() {
			return target;
		}

		public String getAgentName() {
			return agentName;
		}
	}

}
