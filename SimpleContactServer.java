import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.FileInputStream; // Need this for reading the config file!
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// Required for Email (External Library: Jakarta Mail/JavaMail)
// Gotta make sure the Jakarta Mail JARs are in the classpath when running!
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

    // Global config variables. Not final because we load them later from a file.
    private static String SENDER_EMAIL;
    private static String SENDER_PASSWORD; // This MUST be the Gmail App Password, not the regular password!
    private static String RECEIVER_EMAIL = "your mail"; // Hardcoded recipient, but hey, it works.
    private static final int PORT = 5000;
    private static final String CONFIG_FILE = "config.properties"; // The config file name

    public static void main(String[] args) throws IOException {
        
        // Step 1: Load the secret stuff from our config file first!
        loadConfiguration();

        // Safety check to make sure we actually loaded something.
        if (SENDER_EMAIL == null || SENDER_PASSWORD == null || SENDER_EMAIL.isEmpty() || SENDER_PASSWORD.isEmpty()) {
            System.err.println("FATAL ERROR: Email credentials (GMAIL_USER or GMAIL_APP_PASSWORD) are missing.");
            System.err.println("Check " + CONFIG_FILE + " or set environment variables before running!");
            System.exit(1);
        }

        // Step 2: Set up the simple built-in Java web server (HttpServer).
        // It's not as cool as Flask, but it doesn't need external dependencies (besides mail).
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/contact", new ContactHandler()); // Map the URL path
            server.setExecutor(null); // Just use the default executor for simplicity
            server.start();

            System.out.println("------------------------------------------");
            System.out.println("Simple Contact Server is up and running!");
            System.out.println("Listening for POST requests on port " + PORT);
            System.out.println("URL: http://localhost:" + PORT + "/contact");
            System.out.println("------------------------------------------");
        } catch (IOException e) {
            System.err.println("Could not start server on port " + PORT + ". Maybe it's already in use?");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Tries to load GMAIL_USER and GMAIL_APP_PASSWORD from the config.properties file.
     * Falls back to System environment variables if the file fails.
     */
    private static void loadConfiguration() {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            // Reading the properties from the local file
            props.load(input);
            SENDER_EMAIL = props.getProperty("GMAIL_USER");
            SENDER_PASSWORD = props.getProperty("GMAIL_APP_PASSWORD");
            System.out.println("Config loaded successfully from " + CONFIG_FILE);
        } catch (IOException ex) {
            // If the file doesn't exist, or we can't read it, we use the old environment vars as a backup
            System.out.println("Warning: Config file not found or failed to load. Falling back to OS Environment Variables.");
            SENDER_EMAIL = System.getenv("GMAIL_USER");
            SENDER_PASSWORD = System.getenv("GMAIL_APP_PASSWORD");
        }
    }


    /**
     * Handles the HTTP POST request logic. This is where all the web logic lives.
     */
    static class ContactHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Gotta allow CORS or the frontend (portfolio website) will complain!
            setCorsHeaders(exchange);

            String method = exchange.getRequestMethod();

            // Handle the pre-flight check that browsers do for POST requests
            if (method.equalsIgnoreCase("OPTIONS")) {
                sendResponse(exchange, 204, ""); 
                return;
            }

            // Only allow POST requests for the actual form submission
            if (!method.equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"success\": false, \"message\": \"Method Not Allowed\"}");
                return;
            }

            // This is a quick hack to read the form data from the body string
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            Map<String, String> formData = parseFormBody(requestBody);

            String name = formData.get("name");
            String email = formData.get("email");
            String message = formData.get("message");

            System.out.printf("-> New Message Received:\n   Name: %s\n   Email: %s\n", name, email);

            // Basic null/empty validation (just checks if the required fields are there)
            if (name == null || email == null || message == null || name.isEmpty() || email.isEmpty() || message.isEmpty()) {
                sendResponse(exchange, 400, "{\"success\": false, \"message\": \"Missing required fields. Fill everything out!\"}");
                return;
            }

            // Try to send the email!
            try {
                sendContactEmail(name, email, message);
                System.out.println("-> Success! Message sent out. Phew.");
                sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Message sent successfully!\"}");
            } catch (MessagingException e) {
                // Gotta catch those mail exceptions...
                System.err.println("!!! SMTP Error: Failed to send email.");
                e.printStackTrace(); // Print the stack trace so we can debug later
                sendResponse(exchange, 500, "{\"success\": false, \"message\": \"Server error: Could not send email (check server logs).\"}");
            } catch (Exception e) {
                // And catch anything else unexpected!
                System.err.println("!!! Unexpected Server Error!");
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"success\": false, \"message\": \"An unexpected server error occurred.\"}");
            }
        }

        // Just sets the necessary CORS headers so the browser is happy.
        private void setCorsHeaders(HttpExchange exchange) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
        }

        // Standard way to send an HTTP response back to the client.
        private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] responseBytes = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }

        /**
         * Parses form-urlencoded data (key=value&key2=value2) into a Java Map.
         */
        private Map<String, String> parseFormBody(String rawBody) {
            Map<String, String> map = new HashMap<>();
            if (rawBody != null && !rawBody.isEmpty()) {
                for (String pair : rawBody.split("&")) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        try {
                            // URL decode key and value (because of spaces and special chars)
                            String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                            String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                            map.put(key, value);
                        } catch (IOException e) {
                            // If decoding fails, just ignore it and move on.
                            e.printStackTrace(); 
                        }
                    }
                }
            }
            return map;
        }

        /**
         * The JavaMail magic happens here.
         */
        private void sendContactEmail(String name, String fromEmail, String messageContent) throws MessagingException {
            // 1. Mail server properties (Gmail SSL port 465)
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.ssl.enable", "true"); // Always use SSL for security!
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");


            // 2. Create the Session object, using the SENDER credentials for login
            Session session = Session.getInstance(props, new Authenticator() {
                // This is where the app password is used to log into the SMTP server
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            // 3. Construct the email message
            Message message = new MimeMessage(session);

            // Set who it's from and who it's going to
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Portfolio Alert Service"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(RECEIVER_EMAIL));
            message.setSubject("New work call from " + name + " (Urgent!)");
            // Set Reply-To so hitting reply goes to the user who filled the form!
            message.setReplyTo(new InternetAddress[] { new InternetAddress(fromEmail) }); 

            // Create the simple plaintext body
            String body = String.format(
                "Hey!\n\nYou got a new message from your portfolio website. Check it out:\n\n" +
                "Submitted By (Name): %s\n" +
                "Contact Email: %s\n\n" +
                "Their Message:\n" +
                "-----------------------------------------\n" +
                "%s\n" +
                "-----------------------------------------\n" +
                "\n\n- The Java Contact Server Bot",
                name, fromEmail, messageContent
            );

            // Put the text into a body part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body);

            // Wrap the body part in a multipart container (best practice)
            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);

            message.setContent(multipart);

            // 4. Send it! This is the most important line.
            Transport.send(message);
        }
    }
}