package company.vk.edu.distrib.compute.miiishenka.http.exception;

public class UnprocessableContentException extends HttpStatusException {
    @Override
    public int getStatusCode() {
        return 422;
    }
}
