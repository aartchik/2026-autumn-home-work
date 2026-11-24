package company.vk.edu.distrib.compute.robert.api.models;

public enum HttpStatus {
    OK(200),
    CREATED(201),
    ACCEPTED(202),

    MOVED_PERMANENTLY(301),

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    UNPROCESSABLE_CONTENT(422),

    SERVICE_UNAVAILABLE(503);
    
    private final int code;
    
    HttpStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
