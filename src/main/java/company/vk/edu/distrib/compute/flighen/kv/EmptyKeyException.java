package company.vk.edu.distrib.compute.flighen.kv;

public class EmptyKeyException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public EmptyKeyException() {
        super("Key must not be blank");
    }
}
