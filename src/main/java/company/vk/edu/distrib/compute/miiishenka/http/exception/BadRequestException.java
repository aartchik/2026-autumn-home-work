package company.vk.edu.distrib.compute.miiishenka.http.exception;

public class BadRequestException extends HttpStatusException {
    @Override
    public int getStatusCode() {
        return 400;
    }
}
