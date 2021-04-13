import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import jason.asSyntax.parser.TokenMgrError;
import jason.environment.Environment;
import sglib.configuration.SGLibConfigurationBuilder;
import sglib.providers.objectDetection.ObjectDetectionGroundProvider;
import sglib.providers.objectDetection.SGLibObjectDetectionModule;
import sglib.utils.JasonUtils;

public class SGEnvironmentV3 extends Environment {

    private Logger logger = Logger.getLogger("sgmodel_v3." + SGEnvironmentV3.class.getName());
    private boolean serving = false;
    private Object lock = new Object();
    private Timer timer;
    private int simulateChangesIntervalInSeconds = 15;
    private int currentSituation = -1;
    private String[] objectDetectionSituations = {
            "C:\\UFSC\\TCC\\test-images\\1.person.bat",
            "C:\\UFSC\\TCC\\test-images\\2.person_kettle.bat",
            "C:\\UFSC\\TCC\\test-images\\3.person_kettle_cup.bat",
            "C:\\UFSC\\TCC\\test-images\\3.person_kettle_cup.bat",
            "C:\\UFSC\\TCC\\test-images\\4.person_kettle_coffee_cup.bat"
    };
    private Map<String, Literal>[] perceptsSituations;
    private Map<String, Literal> currentDetections = new HashMap<>();

    /** Called before the MAS execution with the args informed in .mas2j */
    @Override
    public void init(String[] args) {
        super.init(args);
        generatePerceptsSituations();

        try {
            configureAgents();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (TokenMgrError e) {
            e.printStackTrace();
        }

        this.timer = new Timer();
        this.timer.schedule(new ChangeSituationTask(), 10 * 1000, simulateChangesIntervalInSeconds * 1000);
    }

    @SuppressWarnings("unchecked")
    private void generatePerceptsSituations() {
        perceptsSituations = (Map<String, Literal>[]) new Map[5];
        perceptsSituations[0] = createPerceptsMap("detected(kettle)");
        perceptsSituations[1] = createPerceptsMap("detected(person)", "detected(coffee_cup)");
        perceptsSituations[2] = createPerceptsMap("detected(person)", "detected(kettle)", "detected(cup)");
        perceptsSituations[3] = createPerceptsMap("detected(person)", "detected(kettle)", "detected(coffee_cup)");
        perceptsSituations[4] = createPerceptsMap("detected(person)", "detected(kettle)", "detected(coffee)");
    }

    private HashMap<String, Literal> createPerceptsMap(String... percepts) {
        HashMap<String, Literal> map = new HashMap<String, Literal>();
        for (String percept : percepts) {
            Literal litPercept = Literal.parseLiteral(percept);
            map.put(JasonUtils.generateLiteralId(litPercept), litPercept);
        }
        return map;
    }

    private void configureAgents() throws ParseException, TokenMgrError {
        Literal cfg = ASSyntax.parseLiteral("configure");
        cfg.addTerm(new StringTermImpl(getConfiguration()));
        addPercept(cfg);
    }

    private String getConfiguration() {
        SGLibObjectDetectionModule module = new SGLibObjectDetectionModule();

        module.setHost("localhost");
        module.setServerPort(50000);
        module.setListenerPort(49999);
        module.setConfigFilePath("D:\\GitHub\\tcc\\object_detection\\config.json");

        return new SGLibConfigurationBuilder().withProvider(ObjectDetectionGroundProvider.class.getTypeName())
                .withModule(module).toJson();
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        String actionName = action.getFunctor();
        if (actionName.equals("configured")) {
            addPercept(agName, Literal.parseLiteral("start"));
            return true;
        } else if (actionName.equals("verifyThatWasSuccessful")) {
            logger.info("The agent is vertifying if it was successful...");
            simulateObjectDetectionChanges(objectDetectionSituations.length - 1);
            informAgsEnvironmentChanged();
            timer.schedule(new FinishServingTask(), 60 * 1000);
            return true;
        } else if (actionName.equals("askTheUtility")) {
            String askedObject = action.getTerm(0).toString();
            logger.info("The agent asked for the utility of the object: " + askedObject);
            answerTheUtilityOf(askedObject);
            return true;
        } else if (actionName.equals("requestObjectToServeWith")) {
            logger.info("The agent requested an object to serve coffee with");
            return true;
        } else if (actionName.equals("requestObjectToServeIn")) {
            logger.info("The agent requested an object to serve coffee in");
            return true;
        } else if (actionName.equals("serveClient")) {
            synchronized (lock) {
                serving = true;
                String objectToServeWith = action.getTerm(0).toString();
                String objectToServeIn = action.getTerm(1).toString();
                logger.info("The agent served the client using " + objectToServeWith + " to fill " + objectToServeIn);
                return true;
            }
        }

        logger.info("executing: " + action + ", but not implemented!");
        return false;
    }

    private void answerTheUtilityOf(String askedObject) {
        String percept = "unkownUtility";
        if (askedObject.equals("teapot") || askedObject.equals("kettle")) {
            percept = "canServeWith";
        } else if (askedObject.equals("mug") || askedObject.equals("coffee_cup") || askedObject.equals("cup")) {
            percept = "canServeIn";
        }
        String newPerceptText = percept + "(" + askedObject + ")";
        logger.info("Answering: " + newPerceptText);
        addPercept(Literal.parseLiteral(newPerceptText));
    }

    private void simulateObjectDetectionChanges() {
        synchronized (lock) {
            if (!serving) {
                simulateObjectDetectionChanges(-1);
            } else {
                logger.info("The agent1 is serving the client, waiting for simulate new changes...");
            }
        }
    }

    private void simulateObjectDetectionChanges(int situationIndex) {
        // Simulates object detection changes for the next execution
        logger.info("Simulating object detection changes...");

        if (situationIndex == -1) {
            currentSituation++;

            if (currentSituation == objectDetectionSituations.length)
                currentSituation = 0;

            situationIndex = currentSituation;
        } else {
            currentSituation = situationIndex;
        }

        String situation = objectDetectionSituations[situationIndex];
        logger.info("Running the situation: " + situation);

        try {
            Runtime.getRuntime().exec("cmd /c start /B " + situation);
            Thread.sleep(1000);
            updatePerceptions();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updatePerceptions() {
        // Changes perceptions using predefined situations to simulate ungrounded
        // percepts
        logger.info("Updating percepts...");
        Map<String, Literal> newDetections = perceptsSituations[currentSituation];

        logger.info(newDetections.size() + " new detections read.");
        ArrayList<Literal> toRemove = new ArrayList<>();
        ArrayList<Literal> toAdd = new ArrayList<>();

        for (String key : newDetections.keySet()) {
            if (!this.currentDetections.containsKey(key)) {
                toAdd.add(newDetections.get(key));
            }
        }

        for (String key : this.currentDetections.keySet()) {
            if (!newDetections.containsKey(key)) {
                toRemove.add(this.currentDetections.get(key));
            }
        }

        logger.info(toRemove.size() + " percepts to remove.");
        for (Literal percept : toRemove) {
            this.currentDetections.remove(JasonUtils.generateLiteralId(percept));
            removePercept(percept);
        }

        logger.info(toAdd.size() + " percepts to add.");
        for (Literal percept : toAdd) {
            this.currentDetections.put(JasonUtils.generateLiteralId(percept), percept);
        }
        addPercept(toAdd.toArray(new Literal[toAdd.size()]));
    }

    private class ChangeSituationTask extends TimerTask {

        @Override
        public void run() {
            simulateObjectDetectionChanges();
        }

    }

    private class FinishServingTask extends TimerTask {

        @Override
        public void run() {
            synchronized (lock) {
                serving = false;
            }
        }

    }
}
