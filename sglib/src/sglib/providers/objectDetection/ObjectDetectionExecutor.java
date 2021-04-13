package sglib.providers.objectDetection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.logging.Logger;

import sglib.configuration.SGLibModule;

public class ObjectDetectionExecutor {

	private SGLibModule module;

	public ObjectDetectionExecutor() {
	}

	public void configure(SGLibModule module) {
		this.module = module;
	}

	public DetectionExecutionResult execute(Logger logger) {
		logger.info("ObjectDetectionExecutor.execute() called");

		DetectionExecutionResult result = new DetectionExecutionResult(DetectionExecutionResultCode.Other, "");
		try {
			sendDetectCommand(logger);

			String response = waitAndReadResponse(logger);

			if (response.equals("ok")) {
				result.setResultCode(DetectionExecutionResultCode.OK);
				result.setMessage("detection returned: ok.");
				logger.info(result.getMessage());
			} else if (response.equals("unchanged")) {
				result.setResultCode(DetectionExecutionResultCode.Unchanged);
				result.setMessage("detection returned: unchanged.");
				logger.info(result.getMessage());
			} else {
				result.setResultCode(DetectionExecutionResultCode.Other);
				result.setMessage("detection do not returned ok. Answer: " + response);
				logger.info(result.getMessage());
			}

		} catch (IOException e) {
			e.printStackTrace();
			result.setResultCode(DetectionExecutionResultCode.Error);
			result.setMessage(e.getMessage());
			logger.severe(result.getMessage());
		}

		logger.info("end of ObjectDetectionExecutor.execute()");

		return result;
	}

	private String waitAndReadResponse(Logger logger) throws IOException {
		int port = (int) module.getConfiguration(
				SGLibObjectDetectionModule.OBJECT_DETECTION_MODULE_LISTENER_PORT_CONFIGURATION_KEY, 49999);

		logger.info("waiting for answer...");
		ServerSocket server = new ServerSocket(port);
		Socket answerConn = server.accept();

		String response = "";

		logger.info("object detection module connected.");
		Scanner sc = new Scanner(answerConn.getInputStream());
		if (sc.hasNextLine()) {
			response = sc.nextLine();
		}

		sc.close();
		server.close();

		return response;
	}

	private void sendDetectCommand(Logger logger) throws IOException {
		String host = (String) module.getConfiguration(
				SGLibObjectDetectionModule.OBJECT_DETECTION_MODULE_HOST_CONFIGURATION_KEY, "localhost");
		int port = (int) module
				.getConfiguration(SGLibObjectDetectionModule.OBJECT_DETECTION_MODULE_PORT_CONFIGURATION_KEY, 50000);

		int maxAttempts = 3;
		for (int i = 1; i <= maxAttempts; i++) {
			logger.info("trying to connecting with object detection module (" + module.getName() + "). Host = " + host
					+ ":" + port + ". Attempts: " + i);
			Socket detectCommandConnection;
			try {
				detectCommandConnection = new Socket(host, port);
				OutputStream saida = detectCommandConnection.getOutputStream();
				saida.write("detect".getBytes(Charset.forName("UTF-8")));
				saida.flush();
				saida.close();
				detectCommandConnection.close();
				logger.info("detect command sent to object detection module.");
				break;
			} catch (IOException e) {
				logger.info(e.getMessage());
				if (i == maxAttempts)
					throw e;
			}
		}

	}

}
