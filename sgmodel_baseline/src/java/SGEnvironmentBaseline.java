import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import sglib.utils.JasonUtils;

public class SGEnvironmentBaseline extends Environment {

    private int currentSituation = -1;
    private Logger logger = Logger.getLogger("sgmodel_baseline." + SGEnvironmentBaseline.class.getName());
    private Map<String, Literal> currentDetections = new HashMap<>();
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
            updatePerceptions();
            informAgsEnvironmentChanged();
            return true;
        } else if (actionName.equals("verifyThatWasSuccessful")) {
            logger.info("The agent is vertifying if it was successful...");
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

    private void updatePerceptions() {
        updatePerceptions(-1);
    }
    
    private void updatePerceptions(int situationIndex) {
        if (situationIndex == -1) {
            currentSituation++;

            if (currentSituation == perceptsSituations.length)
                currentSituation = 0;

            situationIndex = currentSituation;
        } else {
            currentSituation = situationIndex;
        }
        
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
