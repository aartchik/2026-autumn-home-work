package company.vk.edu.distrib.compute.robert.kv.api.v0;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.robert.api.models.AbstractHandler;
import company.vk.edu.distrib.compute.robert.api.models.BadRequestException;
import company.vk.edu.distrib.compute.robert.api.models.HttpStatus;
import company.vk.edu.distrib.compute.robert.api.models.Request;
import company.vk.edu.distrib.compute.robert.api.models.Response;

public class EntityHandler extends AbstractHandler {
    public static final String PATH = "/v0/entity";
    private static final String ID_PARAMETER = "id";

    private final Dao<byte[]> dao;

    public EntityHandler(Dao<byte[]> inputDao) {
        super();
        dao = inputDao;
    }

    // `GET /v0/entity?id=<ID>` -- получить данные по ключу <ID>.
    // Возвращает 200 OK и данные или 404 Not Found.
    @Override
    public Response get(Request request) throws IOException {
        byte[] value = dao.get(extractId(request));
        return Response.builder()
            .setStatus(HttpStatus.OK.code())
            .setBody(value)
            .build();
    }

    // `PUT /v0/entity?id=<ID>` -- создать/перезаписать (upsert) данные по ключу <ID>.
    // Возвращает 201 Created.
    @Override
    public Response put(Request request) throws IOException {
        dao.upsert(extractId(request), request.body());
        return Response.builder()
            .setStatus(HttpStatus.CREATED.code())
            .build();
    }

    // `DELETE /v0/entity?id=<ID>` -- удалить данные по ключу <ID>.
    // Возвращает 202 Accepted.
    @Override
    public Response delete(Request request) throws IOException {
        dao.delete(extractId(request));
        return Response.builder()
            .setStatus(HttpStatus.ACCEPTED.code())
            .build();
    }

    private static String extractId(Request request) {
        if (!PATH.equals(request.path())) {
            throw new NoSuchElementException();
        }

        String id = null;
        for (String parameter : request.query().split("&")) {
            String[] parts = parameter.split("=", 2);
            String name = decode(parts[0]);
            if (ID_PARAMETER.equals(name)) {
                if (id != null || parts.length != 2) {
                    throw new BadRequestException();
                }
                id = decode(parts[1]);
            }
        }

        if (id == null || id.isEmpty()) {
            throw new BadRequestException();
        }
        return id;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
    }
}
