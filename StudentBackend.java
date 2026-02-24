import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StudentBackend {
    //creating a database connection
    private static final String DB_URL = "jdbc:sqlite:students.db"; 
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        // Initialize Database
        initDatabase();

        // Create HttpServer
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Static File Handler
        server.createContext("/", new StaticHandler());

        // API Handlers
        server.createContext("/api/students", new StudentApiHandler());

        server.setExecutor(null); // use default executor
        server.start();
        System.out.println("Server started at http://localhost:" + PORT);
    }

    private static void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(DB_URL);
                    Statement stmt = conn.createStatement()) {
                System.out.println("Connecting to database: " + DB_URL + "...");
                // QUERY 1: Database Initialization - Creates the students table if it doesn't
                // already exist
                stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                        "register_number TEXT PRIMARY KEY," +
                        "name TEXT," +
                        "department TEXT," +
                        "year TEXT," +
                        "phone TEXT," +
                        "email TEXT)");
                System.out.println("Database Initialized and Table Verified.");
            }
        } catch (Exception e) {
            System.err.println("DB Initialization Error: " + e.getMessage());
        }
    }

    // Static File Server Handler
    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/"))
                path = "/index.html";

            File file = new File("public" + path);
            if (file.exists() && !file.isDirectory()) {
                byte[] content = Files.readAllBytes(file.toPath());
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);
                OutputStream os = exchange.getResponseBody();
                os.write(content);
                os.close();
            } else {
                String response = "404 (Not Found)";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html"))
                return "text/html";
            if (path.endsWith(".css"))
                return "text/css";
            if (path.endsWith(".js"))
                return "application/javascript";
            return "text/plain";
        }
    }

    // API Handler for /api/students
    static class StudentApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGet(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handlePost(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDelete(exchange);
                } else {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                }
            } catch (Exception e) {
                e.printStackTrace();
                String response = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        private void handleGet(HttpExchange exchange) throws SQLException, IOException {
            System.out.println("GET /api/students - Fetching student list...");
            List<String> list = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(DB_URL);
                    Statement stmt = conn.createStatement();
                    // QUERY 2: Data Retrieval - Fetches all student records
                    ResultSet rs = stmt.executeQuery("SELECT * FROM students")) {
                while (rs.next()) {
                    String json = String.format(
                            "{\"register_number\":\"%s\",\"name\":\"%s\",\"department\":\"%s\",\"year\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\"}",
                            rs.getString("register_number"),
                            rs.getString("name"),
                            rs.getString("department"),
                            rs.getString("year"),
                            rs.getString("phone"),
                            rs.getString("email"));
                    list.add(json);
                }
            }
            String response = "[" + String.join(",", list) + "]";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private void handlePost(HttpExchange exchange) throws IOException, SQLException {
            String body;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                body = reader.lines().collect(Collectors.joining("\n"));
            }

            // Very simple JSON parsing for this specific flat structure
            String reg = extractJsonValue(body, "register_number");
            String name = extractJsonValue(body, "name");
            String dept = extractJsonValue(body, "department");
            String year = extractJsonValue(body, "year");
            String phone = extractJsonValue(body, "phone");
            String email = extractJsonValue(body, "email");

            if (reg == null || name == null) {
                String response = "Missing required fields";
                exchange.sendResponseHeaders(400, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            System.out.println("POST /api/students - Adding student: " + name + " (" + reg + ")");
            try (Connection conn = DriverManager.getConnection(DB_URL);
                    // QUERY 3: Data Insertion - Adds a new student record to the table
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO students VALUES (?, ?, ?, ?, ?, ?)")) {
                pstmt.setString(1, reg);
                pstmt.setString(2, name);
                pstmt.setString(3, dept);
                pstmt.setString(4, year);
                pstmt.setString(5, phone);
                pstmt.setString(6, email);
                pstmt.executeUpdate();
                System.out.println("Student record saved successfully.");
            }

            String response = "Student added successfully";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private void handleDelete(HttpExchange exchange) throws SQLException, IOException {
            String query = exchange.getRequestURI().getQuery();
            String id = null;
            if (query != null && query.startsWith("id=")) {
                id = query.substring(3);
            }

            if (id == null) {
                String response = "Missing id";
                exchange.sendResponseHeaders(400, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            System.out.println("DELETE /api/students - Deleting student ID: " + id);
            try (Connection conn = DriverManager.getConnection(DB_URL);
                    // QUERY 4: Data Deletion - Removes a record using a unique identifier (Register
                    // Number)
                    PreparedStatement pstmt = conn.prepareStatement("DELETE FROM students WHERE register_number = ?")) {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
                System.out.println("Student record deleted successfully.");
            }

            String response = "Student deleted successfully";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\":\"([^\"]*)\"";
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
            return m.find() ? m.group(1) : "";
        }
    }
}
