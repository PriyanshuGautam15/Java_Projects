# 📧 Java Contact Form Email Server

[![Java](https://img.shields.io/badge/Java-17+-orange?logo=openjdk)](https://openjdk.org/)
[![Jakarta Mail](https://img.shields.io/badge/Jakarta%20Mail-2.0+-blue)](https://eclipse-ee4j.github.io/mail/)

A lightweight Java HTTP server that handles contact form submissions and forwards them via email using Gmail SMTP. No frameworks needed - pure Java!

## 🎯 What I Built

A **backend email service** for portfolio contact forms without using heavy frameworks. Built with Java's native `HttpServer` and Jakarta Mail API to understand how HTTP servers and email protocols work at a fundamental level.

### Key Features
- ✅ Standalone HTTP server (port 5000)
- ✅ Gmail SMTP integration with SSL/TLS
- ✅ CORS support for frontend integration
- ✅ Flexible configuration (file or environment variables)
- ✅ Input validation and error handling
- ✅ Professional email formatting with reply-to headers

## 🛠️ Technologies Used

- **Java 17+**: Core HTTP server and file I/O
- **Jakarta Mail API**: SMTP email functionality
- **Properties API**: Configuration management

### Concepts Applied
- HTTP protocol (requests, responses, status codes, CORS)
- SMTP authentication and SSL/TLS encryption
- URL-encoded form data parsing
- Exception handling and resource management
- REST API design patterns

## 📋 Requirements

- **Java 17+** (JDK)
- **Jakarta Mail API** (jakarta.mail-2.0.1.jar or higher)
- **Gmail Account** with 2FA enabled
- **Gmail App Password** ([How to generate](https://support.google.com/accounts/answer/185833))

### Getting Gmail App Password
1. Enable 2-Factor Authentication on your Google account
2. Go to: Google Account → Security → 2-Step Verification
3. Scroll to "App passwords" and generate one for "Mail"
4. Use this 16-character password in your configuration

## 🚀 Quick Start

### Configuration

**Create config.properties:**
```properties
GMAIL_USER=your-email@gmail.com
GMAIL_APP_PASSWORD=your-16-char-app-password
```

**Or use environment variables:**
```bash
export GMAIL_USER="your-email@gmail.com"
export GMAIL_APP_PASSWORD="your-app-password"
```

### Run the Server

```bash
javac -cp ".:jakarta.mail.jar" SimpleContactServer.java
java -cp ".:jakarta.mail.jar" SimpleContactServer
```

Server starts at: `http://localhost:5000/contact`

### Test with cURL

```bash
curl -X POST http://localhost:5000/contact \
  -d "name=John&email=john@example.com&message=Hello!"
```

## 📡 API Endpoint

### POST /contact

**Request:**
```
name=John&email=john@example.com&message=Your message
```

**Success Response:**
```json
{"success": true, "message": "Message sent successfully!"}
```

**Error Response:**
```json
{"success": false, "message": "Missing required fields!"}
```

## 💡 What I Learned

### Technical Skills
1. **HTTP Server Implementation** - Request routing, headers, status codes
2. **SMTP Protocol** - SSL/TLS configuration, MIME messages, authentication
3. **Configuration Management** - File loading with environment variable fallback
4. **CORS Handling** - Preflight requests, cross-origin headers
5. **Security Best Practices** - Gmail App Passwords, credential management

### Challenges Overcome
- **CORS Issues**: Implemented proper headers and OPTIONS handling
- **SSL Errors**: Configured SSL socket factory for Gmail SMTP (port 465)
- **Form Parsing**: Built custom URL decoder for form data
- **Configuration Flexibility**: Created dual-loading system for different environments

### Code Evolution

**v1**: Hardcoded credentials ❌
**v2**: Environment variables only ⚠️
**v3**: Dual-loading with fallback ✅

This progression shows my understanding of security and deployment best practices.

## 📊 Project Stats

- **Lines of Code**: ~200
- **Development Time**: 8-10 hours
- **Iterations**: 3 major versions
- **Technologies Mastered**: HTTP Server, SMTP, File I/O, CORS

## 🔧 Integration Example

```javascript
// Frontend JavaScript
document.querySelector('form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    
    const response = await fetch('http://localhost:5000/contact', {
        method: 'POST',
        body: new URLSearchParams(formData)
    });
    
    const result = await response.json();
    alert(result.message);
});
```

## 🚀 Future Enhancements

- Rate limiting to prevent spam
- HTML email templates
- Database logging for message history
- reCAPTCHA integration
- Docker containerization

## 📬 Contact

**Priyanshu Gautam**
- GitHub: [@PriyanshuGautam15](https://github.com/PriyanshuGautam15)

---

*Built from scratch to understand backend development and email protocols. No frameworks, just pure Java! ☕*