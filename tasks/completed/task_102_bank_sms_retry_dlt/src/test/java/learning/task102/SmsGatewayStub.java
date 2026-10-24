package learning.task102;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Управляемый шлюз: последовательность ответов и удержание выбранной попытки. */
final class SmsGatewayStub implements AutoCloseable {
    record Call(String method, String path, String contentType, JsonNode body, int attempt, int status) {}
    private record Key(long clientId, String text) {}

    static final class Block {
        private final Set<Long> ids;
        private final String text;
        private final int attempt;
        private final CountDownLatch arrived;
        private final CountDownLatch release = new CountDownLatch(1);

        Block(Set<Long> ids, String text, int attempt, int expected) {
            this.ids = Set.copyOf(ids);
            this.text = text;
            this.attempt = attempt;
            this.arrived = new CountDownLatch(expected);
        }

        boolean matches(Key key, int number) {
            return ids.contains(key.clientId()) && (text == null || text.equals(key.text()))
                    && (attempt == 0 || attempt == number);
        }

        boolean awaitArrivals(int seconds) throws InterruptedException {
            return arrived.await(seconds, TimeUnit.SECONDS);
        }

        void open() { release.countDown(); }
    }

    private final ObjectMapper json = new ObjectMapper();
    private final ConcurrentLinkedQueue<Call> calls = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Key, List<Integer>> plans = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final ExecutorService threads = Executors.newCachedThreadPool(
            Thread.ofPlatform().daemon().name("sms-retry-gateway-test-", 0).factory());
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

    void plan(long clientId, String text, int... statuses) {
        if (statuses.length == 0) throw new IllegalArgumentException("Нужен хотя бы один статус");
        plans.put(new Key(clientId, text), Arrays.stream(statuses).boxed().toList());
    }

    Block block(Set<Long> ids, String text, int expected) {
        return setBlock(new Block(ids, text, 0, expected));
    }

    Block blockAttempt(long clientId, String text, int attempt) {
        if (attempt < 1) throw new IllegalArgumentException("Попытки начинаются с 1");
        return setBlock(new Block(Set.of(clientId), text, attempt, 1));
    }

    private Block setBlock(Block next) {
        release();
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

    List<Call> callsFor(long clientId, String text) {
        return callsFor(clientId).stream().filter(call -> text.equals(call.body().path("text").asText())).toList();
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
            Key key = new Key(body.path("clientId").asLong(Long.MIN_VALUE), body.path("text").asText());
            int number = attempts.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
            List<Integer> statuses = plans.getOrDefault(key, List.of(204));
            int status = statuses.get(Math.min(number - 1, statuses.size() - 1));
            calls.add(new Call(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Content-Type"), body, number, status));
            Block current = block;
            if (current != null && current.matches(key, number)) {
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
            exchange.sendResponseHeaders(status, -1);
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
