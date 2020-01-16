package ca.bc.gov.educ.api.soam.exception;

public class SoamRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6586840928883479043L;

	public SoamRuntimeException() {
	}

	public SoamRuntimeException(String message) {
		super(message);
	}

	public SoamRuntimeException(Throwable cause) {
		super(cause);
	}

	public SoamRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public SoamRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
