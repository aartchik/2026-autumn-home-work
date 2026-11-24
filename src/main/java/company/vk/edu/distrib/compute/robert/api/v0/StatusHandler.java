package company.vk.edu.distrib.compute.robert.api.v0;

import company.vk.edu.distrib.compute.robert.api.models.AbstractHandler;
import company.vk.edu.distrib.compute.robert.api.models.HttpStatus;
import company.vk.edu.distrib.compute.robert.api.models.Request;
import company.vk.edu.distrib.compute.robert.api.models.Response;

public class StatusHandler extends AbstractHandler {
    public static final String PATH = "/v0/status";

    @Override
    public Response get(Request request) {
        return Response.builder()
            .setStatus(HttpStatus.OK.code())
            .build();
    }
}
