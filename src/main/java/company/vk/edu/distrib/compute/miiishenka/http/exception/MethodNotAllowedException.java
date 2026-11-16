package company.vk.edu.distrib.compute.miiishenka.http.exception;

public class MethodNotAllowedException extends HttpStatusException {
    @Override
    public int getStatusCode() {
        return 405;
    }
}
