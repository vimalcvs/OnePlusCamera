package camera.vimal.vk;


public class HDRProcessorException extends Exception {
	final static public int INVALID_N_IMAGES = 0;
	final static public int UNEQUAL_SIZES = 1;

	final private int code;

	HDRProcessorException(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
