# AI Assistant Setup Guide

This guide covers everything needed to get the AI Assistant feature working in the Afetch application.

## Prerequisites

1. **Docker Desktop** - Download from [docker.com](https://www.docker.com/products/docker-desktop)
2. **OpenRouter API Key** - Get for free from [openrouter.ai/auth/keys](https://openrouter.ai/auth/keys)
3. **Maven** - For building the Java application (included as `mvnw`)

## Configuration

### 1. Set Environment Variables

Create a `.env` file in the project root (copy from `.env.example`):

```bash
cp .env.example .env
```

Edit `.env` and add your OpenRouter API key:

```
OPENROUTER_API_KEY=your_actual_api_key_here
```

### 2. Start Docker Services

```bash
docker-compose up --build
```

This starts:
- Spring Boot application (port 8081)
- MySQL database
- Redis cache
- Nginx reverse proxy

**Expected startup output:**
```
mysql    | MySQL ready for connections
redis    | Ready to accept connections
app      | Tomcat started on port 8080
```

### 3. Verify Services

- Application: http://localhost:8081
- MySQL: localhost:3306
- Redis: localhost:6379

## AI Assistant Features

### Conversation Management

- **Create New Chat**: Click "New chat" button in sidebar
- **Resume Previous Chat**: Click any conversation in sidebar to load history
- **Message Persistence**: All messages saved to MySQL database

### Multi-Language Support

The AI assistant supports:
- **English**: Default language, optimized for programming help
- **Russian**: Full technical and professional responses
- **Armenian**: Full technical and professional responses

Send messages in any supported language and get responses in the same language.

### YSCI Knowledge Base

Questions about Yerevan State College of Informatics are answered with relevant institutional knowledge.

Example queries:
- "Tell me about YSCI departments"
- "Որ մասնագիտություններ կան YSCI-ում?" (Armenian)
- "Какие специальности в YSCI?" (Russian)

### Professional Programming Assistance

The system is optimized for:
- Code review and debugging
- Architecture and design patterns
- Best practices and optimization
- Algorithm and data structure problems
- Multi-language solutions (Python, Java, JavaScript, C++, etc.)

### Rate Limiting

- **Limit**: 30 AI requests per hour per user
- **Enforcement**: Redis-based caching
- **Error Message**: "Rate limit exceeded. Maximum 30 requests per hour."

## Troubleshooting

### AI Not Responding

**Check:**
1. Verify OpenRouter API key in `.env`: `echo $OPENROUTER_API_KEY`
2. Check Docker logs: `docker-compose logs app`
3. Verify Redis: `docker-compose logs redis`

**Error: "OpenRouter API key is not configured"**
- Solution: Set `OPENROUTER_API_KEY` environment variable in `.env`

**Error: "OpenRouter API error: 401"**
- Solution: API key is invalid. Get a new one from [openrouter.ai/auth/keys](https://openrouter.ai/auth/keys)

### Database Issues

**Error: "Unable to connect to database"**
- Verify MySQL is running: `docker-compose ps`
- Check MySQL logs: `docker-compose logs mysql`
- Ensure port 3306 is available

### Redis Connection Issues

**Error: "Connection refused" for Redis**
- Verify Redis is running: `docker-compose ps`
- Check Redis logs: `docker-compose logs redis`

## Development

### Building Locally

```bash
./mvnw clean package
```

### Running Without Docker

For local development (requires MySQL 8.0+ and Redis locally installed):

```bash
./mvnw spring-boot:run
```

### Database Migrations

Flyway automatically applies migrations on startup:
- `V1__initial_schema.sql` - Core schema including AI tables
- `V2__seed_builtin_rooms.sql` - Default chat rooms
- `V3__fix_ai_message_role_enum.sql` - AI message role fixes
- `V4__convert_chatroom_type_to_enum.sql` - ChatRoom type enum

## API Endpoints

### Chat Operations

- **POST /api/ai/chat** - Send message to AI
  ```json
  {
    "message": "Your question here",
    "conversationId": null  // omit for new chat, include for existing
  }
  ```
  Response:
  ```json
  {
    "reply": "AI response",
    "conversationId": 123
  }
  ```

- **GET /api/ai/conversations** - List all user's conversations
  Response:
  ```json
  [
    {
      "id": 123,
      "createdAt": "2024-01-15T10:30:00Z",
      "messagesCount": 5
    }
  ]
  ```

- **GET /api/ai/conversations/{id}** - Get specific conversation with messages
  Response:
  ```json
  {
    "id": 123,
    "createdAt": "2024-01-15T10:30:00Z",
    "messages": [
      {
        "id": 1,
        "role": "user",
        "content": "Hello",
        "createdAt": "2024-01-15T10:30:00Z"
      },
      {
        "id": 2,
        "role": "assistant",
        "content": "Hi there!",
        "createdAt": "2024-01-15T10:31:00Z"
      }
    ]
  }
  ```

## Database Schema

### AI Tables

```sql
-- Conversations
CREATE TABLE ai_conversations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Messages
CREATE TABLE ai_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,  -- 'user', 'assistant', 'system'
  content TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE
);
```

## Performance Considerations

1. **Caching**: Responses are cached in Redis for 5 minutes
2. **Rate Limiting**: 30 requests per hour prevents API quota exhaustion
3. **Message Loading**: Lazy-loads conversation history on demand
4. **Connection Pooling**: HikariCP manages database connections efficiently

## Security

- All AI endpoints require JWT authentication
- API key stored in environment variables (not in code)
- Messages associated with authenticated users only
- SQL injection prevented via JPA parameterized queries
- CSRF protection enabled in Spring Security

## Additional Resources

- [OpenRouter Documentation](https://openrouter.ai/docs)
- [Spring Boot Security](https://spring.io/projects/spring-security)
- [Flyway Database Migrations](https://flywaydb.org/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
