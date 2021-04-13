import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import sglib.configuration.SGLibConfiguration;
import sglib.configuration.SGLibConfigurationBuilder;
import sglib.environments.BaseGroundedPerceptTargetEnvironment;
import sglib.providers.IGroundedPerceptProvider;
import sglib.providers.IGroundedPerceptTarget;
import sglib.providers.objectDetection.ObjectDetectionGroundProvider;
import sglib.providers.objectDetection.ObjectDetectionGroundedPerceptProvider;
import sglib.providers.objectDetection.SGLibObjectDetectionModule;

public class SGEnvironmentV2 extends BaseGroundedPerceptTargetEnvironment implements IGroundedPerceptTarget {

    private boolean serving = false;
    private Object lock = new Object();
    private Timer timer;
    private int simulateChangesIntervalInSeconds = 15;
    private Logger logger = Logger.getLogger("sgmodel_v2." + SGEnvironmentV2.class.getName());
    private int currentSituation = -1;
    private String[] objectDetectionSituations = {
            "C:\\UFSC\\TCC\\test-images\\1.person.bat",
            "C:\\UFSC\\TCC\\test-images\\2.person_kettle.bat",
            "C:\\UFSC\\TCC\\test-images\\3.person_kettle_cup.bat",
            "C:\\UFSC\\TCC\\test-images\\4.person_kettle_coffee_cup.bat"
    };

    /** Called before the MAS execution with the args informed in .mas2j */
    @Override
    public void init(String[] args) {
        super.init(args);
        this.timer = new Timer();
        this.timer.schedule(new ChangeSituationTask(), 0, simulateChangesIntervalInSeconds * 1000);
    }

    @Override
    protected SGLibConfiguration createSGLibConfiguration() {
        SGLibObjectDetectionModule module = new SGLibObjectDetectionModule();

        module.setHost("localhost");
        module.setServerPort(50000);
        module.setListenerPort(49999);
        module.setConfigFilePath("D:\\GitHub\\tcc\\object_detection\\config.json");

        return new SGLibConfigurationBuilder().withProvider(ObjectDetectionGroundProvider.class.getTypeName())
                .withModule(module).build();
    }

    @Override
    protected void loadProvidersAndRegister() {
        IGroundedPerceptProvider provider = new ObjectDetectionGroundedPerceptProvider();
        provider.register(this, null);
        this.providers.add(provider);
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        String actionName = action.getFunctor();
        if (actionName.equals("verifyThatWasSuccessful")) {
            synchronized (lock) {
                logger.info("The agent is vertifying if it was successful...");
                simulateObjectDetectionChanges(objectDetectionSituations.length - 1);
                informAgsEnvironmentChanged();
                serving = false;
                return true;
            }
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

    private class ChangeSituationTask extends TimerTask {

        @Override
        public void run() {
            simulateObjectDetectionChanges();
        }

    }
}
