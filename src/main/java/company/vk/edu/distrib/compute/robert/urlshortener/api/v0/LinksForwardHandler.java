package company.vk.edu.distrib.compute.robert.urlshortener.api.v0;

import java.io.IOException;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.robert.api.models.AbstractHandler;
import company.vk.edu.distrib.compute.robert.api.models.HttpStatus;
import company.vk.edu.distrib.compute.robert.api.models.Request;
import company.vk.edu.distrib.compute.robert.api.models.Response;

// * Во всех случаях, когда передаётся либо не валидный `<ID>` либо невалидная ссылка в теле запроса (POST/PUT методы)
//  - надо вернуть `422 Unprocessable Content`
public class LinksForwardHandler extends AbstractHandler {        
    public static final String PATH = "/v0/links/";

    private final Dao<String> dao;

    public LinksForwardHandler(Dao<String> inputDao) {
        super();
        dao = inputDao;
    }

    // * `GET /v0/links/<ID>` -- получить длинную ссылку по `<ID>` короткой ссылки. Возвращает `200 OK`, 
    // `Content-Type: text/html; charset=utf-8` и ссылку или `404 Not Found`. 
    @Override
    public Response get(Request request) throws NoSuchElementException, IOException {
        String id = request.path().substring(PATH.length());

        String longLink = dao.get(id);

        return Response.builder()
            .setStatus(HttpStatus.OK.code())
            .putHeader("Content-Type", "text/html; charset=utf-8")
            .setBody(longLink)
            .build();
    }

    // * `PUT /v0/links/<ID>` -- изменить существующую короткую ссылку по `<ID>` на заданную в теле, 
    // `Content-Type: text/html; charset=utf-8`. Возвращает `200 OK`, или `404 Not found` если такого `<ID>` нет.
    @Override
    public Response put(Request request) throws NoSuchElementException, IOException {
        String id = request.path().substring(PATH.length());
        String longLink = request.bodyAsString();

        dao.get(id);
        dao.upsert(id, longLink);
      
        return Response.builder()
            .setStatus(HttpStatus.OK.code())
            .build();
    }

    // * `DELETE /v0/links/<ID>` -- удалить ссылку по `<ID>`. Возвращает `202 Accepted`.
    @Override
    public Response delete(Request request) throws NoSuchElementException, IOException {
        String id = request.path().substring(PATH.length());

        dao.delete(id);

        return Response.builder()
            .setStatus(HttpStatus.ACCEPTED.code())
            .build();
    }

}
