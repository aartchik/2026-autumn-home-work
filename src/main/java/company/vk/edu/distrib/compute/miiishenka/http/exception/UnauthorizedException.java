package company.vk.edu.distrib.compute.miiishenka.http.exception;

public class UnauthorizedException extends HttpStatusException {
    @Override
    public int getStatusCode() {
        return 401;
    }
}
