package company.vk.edu.distrib.compute.robert.urlshortener.api.v0;

import java.io.IOException;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.robert.api.models.AbstractHandler;
import company.vk.edu.distrib.compute.robert.api.models.HttpStatus;
import company.vk.edu.distrib.compute.robert.api.models.Request;
import company.vk.edu.distrib.compute.robert.api.models.Response;

// * Во всех случаях, когда передаётся либо не валидный `<ID>` либо невалидная ссылка в теле запроса (POST/PUT методы)
// - надо вернуть `422 Unprocessable Content`
public class ForwardHandler extends AbstractHandler {
    public static final String PATH = "/";

    private final Dao<String> dao;

    public ForwardHandler(Dao<String> inputDao) {
        super();
        dao = inputDao;
    }
    
    // * `GET /<ID>` -- отдаётся редирект `301 Moved Permanently` и заголовок 
    // `Location: <длинная ссылка соотвествующая ID>`.
    // `404 Not Found` если такого `<ID>` нет.
    @Override
    public Response get(Request request) throws IOException {
        String id = request.path().substring(PATH.length());

        String longLink = dao.get(id);

        return Response.builder()
            .setStatus(HttpStatus.MOVED_PERMANENTLY.code())
            .putHeader("Location", longLink)
            .build();
    }
    
}
