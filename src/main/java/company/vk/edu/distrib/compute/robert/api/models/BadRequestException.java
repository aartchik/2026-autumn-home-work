package company.vk.edu.distrib.compute.robert.api.models;

public class BadRequestException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public BadRequestException() {
        super();
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }
}
