import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import sglib.utils.JasonUtils;

public class SGEnvironmentV1 extends Environment {

    private Map<String, Literal> currentDetections = new HashMap<>();
    private Logger logger = Logger.getLogger("sgmodel_v1." + SGEnvironmentV1.class.getName());
    private int currentSituation = -1;
    private String[] objectDetectionSituations = {
            "C:\\UFSC\\TCC\\test-images\\1.person.bat",
            "C:\\UFSC\\TCC\\test-images\\2.person_kettle.bat",
            "C:\\UFSC\\TCC\\test-images\\3.person_kettle_cup.bat",
            "C:\\UFSC\\TCC\\test-images\\3.person_kettle_cup.bat",
            "C:\\UFSC\\TCC\\test-images\\4.person_kettle_coffee_cup.bat"
    };

    private Map<String, Literal>[] perceptsSituations;

    @Override
    public void init(String[] args) {
        super.init(args);
        generatePerceptsSituations();
    }

    @SuppressWarnings("unchecked")
    private void generatePerceptsSituations() {
        perceptsSituations = (Map<String, Literal>[]) new Map[5];
        perceptsSituations[0] = createPerceptsMap("detected(kettle)");
        perceptsSituations[1] = createPerceptsMap("detected(person)", "detected(coffee_cup)");
        perceptsSituations[2] = createPerceptsMap("detected(person)", "detected(kettle)", "detected(cup)");
        perceptsSituations[3] = createPerceptsMap("detected(person)", "detected(kettle)", "detected(coffee_cup)");
        perceptsSituations[4] = createPerceptsMap("detected(person)", "detected(kettle)", "detected(coffee_cup)", "detected(coffee)");
    }

    private HashMap<String, Literal> createPerceptsMap(String... percepts) {
        HashMap<String, Literal> map = new HashMap<String, Literal>();
        for (String percept : percepts) {
            Literal litPercept = Literal.parseLiteral(percept);
            map.put(JasonUtils.generateLiteralId(litPercept), litPercept);
        }
        return map;
    }

    @Override
    public boolean executeAction(String agName, Structure action) {

        String actionName = action.getFunctor();
        if (actionName.equals("refreshPerceptions")) {
            logger.info("The agent is refreshing the percepts...");
            simulateObjectDetectionChanges();
            updatePerceptions();
            informAgsEnvironmentChanged();
            return true;
        } else if (actionName.equals("verifyThatWasSuccessful")) {
            logger.info("The agent is vertifying if it was successful...");
            simulateObjectDetectionChanges(objectDetectionSituations.length - 1);
            updatePerceptions();
            informAgsEnvironmentChanged();
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
            String objectToServeWith = action.getTerm(0).toString();
            String objectToServeIn = action.getTerm(1).toString();
            logger.info("The agent served the client using " + objectToServeWith + " to fill " + objectToServeIn);
            return true;
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
        simulateObjectDetectionChanges(-1);
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

}
