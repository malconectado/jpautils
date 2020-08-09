package com.malconectado.exception;

//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestParamException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7327588378614941853L;

	public BadRequestParamException() {
		
	}

	public BadRequestParamException(String arg0) {
		super(arg0);
		
	}

	public BadRequestParamException(Throwable arg0) {
		super(arg0);
		
	}

	public BadRequestParamException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		
	}

	public BadRequestParamException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
		
	}

}
