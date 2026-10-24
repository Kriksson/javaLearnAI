package learning.task101;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Локальный учебный HTTP-шлюз. Полностью принадлежит инфраструктуре тестов. */
final class SmsGatewayStub implements AutoCloseable {
    record Call(String method, String contentType, JsonNode body) {}

    static final class Block {
        private final Set<Long> ids;
        private final String onlyText;
        private final CountDownLatch arrived;
        private final CountDownLatch release = new CountDownLatch(1);

        Block(Set<Long> ids, String onlyText, int expected) {
            this.ids = Set.copyOf(ids);
            this.onlyText = onlyText;
            this.arrived = new CountDownLatch(expected);
        }

        boolean matches(JsonNode body) {
            return ids.contains(body.path("clientId").asLong(Long.MIN_VALUE))
                    && (onlyText == null || onlyText.equals(body.path("text").asText()));
        }

        boolean awaitArrivals(int seconds) throws InterruptedException {
            return arrived.await(seconds, TimeUnit.SECONDS);
        }

        void open() { release.countDown(); }
    }

    private final ObjectMapper json = new ObjectMapper();
    private final ConcurrentLinkedQueue<Call> calls = new ConcurrentLinkedQueue<>();
    private final ExecutorService threads = Executors.newCachedThreadPool(
            Thread.ofPlatform().daemon().name("sms-gateway-test-", 0).factory());
    private final HttpServer server;
    private volatile Block block;

    private SmsGatewayStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/gateway/sms", this::handle);
        server.setExecutor(threads);
        server.start();
    }

    static SmsGatewayStub start() {
        try {
            return new SmsGatewayStub();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/gateway/sms";
    }

    Block block(Set<Long> ids, String onlyText, int expected) {
        release();
        Block next = new Block(ids, onlyText, expected);
        block = next;
        return next;
    }

    void release() {
        Block previous = block;
        if (previous != null) previous.open();
        block = null;
    }

    List<Call> callsFor(long clientId) {
        return calls.stream().filter(call -> call.body().path("clientId").asLong(Long.MIN_VALUE) == clientId).toList();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            byte[] raw = exchange.getRequestBody().readAllBytes();
            JsonNode body;
            try {
                body = json.readTree(raw);
                if (body == null) body = json.nullNode();
            } catch (IOException invalidJson) {
                body = json.createObjectNode().put("invalidJson", new String(raw, StandardCharsets.UTF_8));
            }
            calls.add(new Call(exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("Content-Type"), body));
            Block current = block;
            if (current != null && current.matches(body)) {
                current.arrived.countDown();
                try {
                    if (!current.release.await(20, TimeUnit.SECONDS)) {
                        exchange.sendResponseHeaders(503, -1);
                        return;
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            exchange.sendResponseHeaders(204, -1);
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        release();
        server.stop(0);
        threads.shutdownNow();
    }
}
