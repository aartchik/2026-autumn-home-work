package company.vk.edu.distrib.compute.miiishenka.http.exception;

public class NotFoundException extends HttpStatusException {
    @Override
    public int getStatusCode() {
        return 404;
    }
}
