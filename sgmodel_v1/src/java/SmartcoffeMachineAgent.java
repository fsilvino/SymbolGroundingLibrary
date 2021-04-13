
import java.util.ArrayList;

import sglib.agents.BaseGroundedAgent;
import sglib.configuration.SGLibConfiguration;
import sglib.configuration.SGLibConfigurationBuilder;
import sglib.providers.IGroundProvider;
import sglib.providers.objectDetection.ObjectDetectionGroundProvider;
import sglib.providers.objectDetection.SGLibObjectDetectionModule;

public class SmartcoffeMachineAgent extends BaseGroundedAgent {

    private static final long serialVersionUID = 1L;

    public SmartcoffeMachineAgent() {
        super();
    }

    @Override
    protected SGLibConfiguration createSGLibConfiguration() {
        SGLibObjectDetectionModule module = new SGLibObjectDetectionModule();

        module.setHost("localhost");
        module.setServerPort(50000);
        module.setListenerPort(49999);
        module.setConfigFilePath("D:\\GitHub\\tcc\\object_detection\\config.json");

        return new SGLibConfigurationBuilder().withModule(module).build();
    }

    @Override
    protected ArrayList<IGroundProvider> createGroundProviders() {
        ArrayList<IGroundProvider> result = new ArrayList<IGroundProvider>();
        result.add(new ObjectDetectionGroundProvider());
        return result;
    }

}
