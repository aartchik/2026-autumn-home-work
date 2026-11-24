package company.vk.edu.distrib.compute.robert.urlshortener.api.v0;

import java.io.IOException;

import company.vk.edu.distrib.compute.robert.api.models.AbstractHandler;
import company.vk.edu.distrib.compute.robert.api.models.HttpStatus;
import company.vk.edu.distrib.compute.robert.api.models.Request;
import company.vk.edu.distrib.compute.robert.api.models.Response;
import company.vk.edu.distrib.compute.robert.dao.RobertDao;
import company.vk.edu.distrib.compute.robert.urlshortener.utils.Credentials;

// * Во всех случаях, когда передаётся либо не валидный `<ID>` либо невалидная ссылка в теле запроса (POST/PUT методы)
// - надо вернуть `422 Unprocessable Content`
public class InternalUserHandler extends AbstractHandler {
    public static final String PATH = "/internal/users";

    private final RobertDao dao;

    public InternalUserHandler(RobertDao inputDao) {
        super();
        dao = inputDao;
    }
    
    // 3. Добавить в HTTP API протокол сервиса: `POST /internal/users` -- добавить пользователя, 
    // `Content-Type: text/html; charset=utf-8`, тело состоит из одной строки содержащей имя пользователя
    // и пароль разделённые двоеточием (например `admin:super_pass`). Возвращает `200 OK`, если пользователь 
    // уже есть заменить пароль на заданный. Метод нужен, чтобы можно было наполнить базу пользователей для простоты
    // тестирования. В реальных сервисах такое делается по-другому.
    @Override
    public Response post(Request request) throws IOException {
        Credentials creds = Credentials.from(request.bodyAsString());

        dao.upsert(creds.user(), creds.password());

        return Response.builder()
            .setStatus(HttpStatus.OK.code())
            .build();
    }
}
