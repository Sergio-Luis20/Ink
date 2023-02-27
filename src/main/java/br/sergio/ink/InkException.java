package br.sergio.ink;

public class InkException extends Exception {
    
    public InkException() {
        super();
    }

    public InkException(String message) {
        super(message);
    }

    public InkException(Throwable cause) {
        super(cause);
    }

    public InkException(String message, Throwable cause) {
        super(cause);
    }

}
