package sglib.providers.objectDetection;

public class DetectionExecutionResult {

	private DetectionExecutionResultCode resultCode;
	private String message;

	public DetectionExecutionResult() {
	}

	public DetectionExecutionResult(DetectionExecutionResultCode resultCode, String message) {
		this.resultCode = resultCode;
		this.message = message;
	}

	public DetectionExecutionResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(DetectionExecutionResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
