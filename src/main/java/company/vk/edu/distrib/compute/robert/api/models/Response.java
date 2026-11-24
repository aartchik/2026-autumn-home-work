package company.vk.edu.distrib.compute.robert.api.models;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record Response(
    int status,
    Map<String, String> headers,
    byte[] body
) {
    public Response {
        body = body.clone();
        headers = Map.copyOf(headers);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int status;
        private final Map<String, String> headers = new ConcurrentHashMap<>();
        private byte[] body = new byte[0];

        public Builder setStatus(int value) {
            status = value;
            return this;
        }

        public Builder putHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder setBody(byte[] value) {
            body = value.clone();
            return this;
        }

        public Builder setBody(String value) {
            body = value.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        public Response build() {
            return new Response(
                this.status,
                Map.copyOf(this.headers),
                this.body
            );
        }
    }
}
