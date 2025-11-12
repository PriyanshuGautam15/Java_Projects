import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeBodyPart;

public class SimpleContactServer {

    // Config values from file ya environment
    private static String SENDER_EMAIL;
    private static String SENDER_PASSWORD;
    private static String RECEIVER_EMAIL = "your mail";
    private static final int PORT = 5000;
    private static final String CONFIG_FILE = "config.properties";

    public static void main(String[] args) throws IOException {
        // Load email credentials first
        loadConfiguration();

        // Check if credentials mil gaye ya nahi
        if (SENDER_EMAIL == null || SENDER_PASSWORD == null || SENDER_EMAIL.isEmpty() || SENDER_PASSWORD.isEmpty()) {
            System.err.println("FATAL ERROR: Email credentials missing!");
            System.exit(1);
        }

        // Start karte hain simple HTTP server
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/contact", new ContactHandler());
            server.setExecutor(null);
            server.start();

            System.out.println("------------------------------------------");
            System.out.println("Server chal gaya! Port: " + PORT);
            System.out.println("Visit: http://localhost:" + PORT + "/contact");
            System.out.println("------------------------------------------");
        } catch (IOException e) {
            System.err.println("Port " + PORT + " already in use ho sakta hai!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Load kare config file se Gmail user aur password
    private static void loadConfiguration() {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            props.load(input);
            SENDER_EMAIL = props.getProperty("GMAIL_USER");
            SENDER_PASSWORD = props.getProperty("GMAIL_APP_PASSWORD");
            System.out.println("Config file loaded successfully!");
        } catch (IOException ex) {
            System.out.println("Config file nahi mili, environment se le raha hu...");
            SENDER_EMAIL = System.getenv("GMAIL_USER");
            SENDER_PASSWORD = System.getenv("GMAIL_APP_PASSWORD");
        }
    }

    // Ye handle karega /contact route
    static class ContactHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Browser ko CORS allow karna zaruri hai
            setCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            // OPTIONS request ke liye direct blank response
            if (method.equalsIgnoreCase("OPTIONS")) {
                sendResponse(exchange, 204, "");
                return;
            }

            // Sirf POST hi allowed hai
            if (!method.equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"success\": false, \"message\": \"Method Not Allowed\"}");
                return;
            }

            // Form data read karte hain
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            Map<String, String> formData = parseFormBody(requestBody);

            String name = formData.get("name");
            String email = formData.get("email");
            String message = formData.get("message");

            System.out.printf("-> New Message:\n   Name: %s\n   Email: %s\n", name, email);

            // Check karo sab fields filled hain
            if (name == null || email == null || message == null || name.isEmpty() || email.isEmpty() || message.isEmpty()) {
                sendResponse(exchange, 400, "{\"success\": false, \"message\": \"Missing required fields!\"}");
                return;
            }

            // Ab email bhejne ka try karte hain
            try {
                sendContactEmail(name, email, message);
                System.out.println("-> Message sent successfully!");
                sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Message sent successfully!\"}");
            } catch (MessagingException e) {
                System.err.println("SMTP error: Email send nahi hua!");
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"success\": false, \"message\": \"Email send failed!\"}");
            } catch (Exception e) {
                System.err.println("Kuch unexpected error aa gaya!");
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"success\": false, \"message\": \"Unexpected server error!\"}");
            }
        }

        // CORS headers set karne ka method
        private void setCorsHeaders(HttpExchange exchange) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
        }

        // Client ko response bhejne ka simple method
        private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] responseBytes = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }

        // Form data ko map me convert karne ka simple function
        private Map<String, String> parseFormBody(String rawBody) {
            Map<String, String> map = new HashMap<>();
            if (rawBody != null && !rawBody.isEmpty()) {
                for (String pair : rawBody.split("&")) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        try {
                            String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                            String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                            map.put(key, value);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return map;
        }

        // Ye function actual email bhejta hai Gmail ke through
        private void sendContactEmail(String name, String fromEmail, String messageContent) throws MessagingException {
            // Gmail SMTP setup
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

            // Session create karo with password authentication
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            // Message create karte hain
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Portfolio Alert Service"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(RECEIVER_EMAIL));
            message.setSubject("New work call from " + name + " (Urgent!)");
            message.setReplyTo(new InternetAddress[] { new InternetAddress(fromEmail) });

            // Body content banate hain
            String body = String.format(
                "Hey!\n\nYou got a new message from your portfolio website:\n\n" +
                "Name: %s\n" +
                "Email: %s\n\n" +
                "Message:\n" +
                "-----------------------------------------\n" +
                "%s\n" +
                "-----------------------------------------\n" +
                "\n\n- Java Contact Server Bot",
                name, fromEmail, messageContent
            );

            // Add text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body);

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);

            message.setContent(multipart);

            // Send kar diya!
            Transport.send(message);
        }
    }
}
