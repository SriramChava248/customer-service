#!/bin/bash

# ============================================================================
# JWT Token Generator Script
# ============================================================================
# 
# Usage:
#   1. Edit the USER_CONFIG section below to set userId, email, and role
#   2. Run: ./generate-token.sh
#   3. Copy the generated token and use it in your curl commands
#
# ============================================================================

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# USER_CONFIG - Edit these values as needed
# ============================================================================
USER_ID="2"
EMAIL="bob.3wilson@example.com"
ROLE="ADMIN"  # Options: "CUSTOMER" or "ADMIN"

# ============================================================================
# Configuration (reads from application.properties)
# ============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROPERTIES_FILE="$SCRIPT_DIR/src/main/resources/application.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
    echo -e "${YELLOW}Warning: application.properties not found, using defaults${NC}"
    SECRET="your-secret-key-change-in-production-minimum-32-characters-long"
    ISSUER="api-gateway"
    EXPIRATION_MS="3600000"
else
    SECRET=$(grep "^security.jwt.secret=" "$PROPERTIES_FILE" | cut -d'=' -f2-)
    ISSUER=$(grep "^security.jwt.issuer=" "$PROPERTIES_FILE" | cut -d'=' -f2-)
    EXPIRATION_MS=$(grep "^security.jwt.expiration-ms=" "$PROPERTIES_FILE" | cut -d'=' -f2-)
    
    # Default values if not found
    SECRET=${SECRET:-"your-secret-key-change-in-production-minimum-32-characters-long"}
    ISSUER=${ISSUER:-"api-gateway"}
    EXPIRATION_MS=${EXPIRATION_MS:-"3600000"}
fi

# ============================================================================
# Validate inputs
# ============================================================================
if [ -z "$USER_ID" ] || [ -z "$EMAIL" ] || [ -z "$ROLE" ]; then
    echo -e "${YELLOW}Error: USER_ID, EMAIL, and ROLE must be set in the script${NC}"
    exit 1
fi

# ============================================================================
# Generate JWT Token using Maven exec:java
# ============================================================================
cd "$SCRIPT_DIR" || exit 1

echo -e "${BLUE}Generating JWT token...${NC}"

# Create a simple Java class inline and execute it
TOKEN=$(mvn -q exec:java \
    -Dexec.mainClass="io.jsonwebtoken.Jwts" \
    -Dexec.classpathScope="compile" \
    -Dexec.args="" 2>/dev/null)

# Alternative: Use a simple Java one-liner approach
# Create temporary Java source file
TEMP_DIR=$(mktemp -d)
TEMP_JAVA="$TEMP_DIR/TokenGen.java"

cat > "$TEMP_JAVA" << EOF
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TokenGen {
    public static void main(String[] args) {
        String secret = "$SECRET";
        String issuer = "$ISSUER";
        long expirationMs = ${EXPIRATION_MS}L;
        String userId = "$USER_ID";
        String email = "$EMAIL";
        String role = "$ROLE";
        
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);
        
        String token = Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuer(issuer)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
        
        System.out.print(token);
    }
}
EOF

# Compile and run
if command -v javac &> /dev/null && [ -d "target/classes" ]; then
    # Get classpath
    CLASSPATH=$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout 2>/dev/null)
    CLASSPATH="target/classes:$CLASSPATH"
    
    # Compile
    javac -cp "$CLASSPATH" -d "$TEMP_DIR" "$TEMP_JAVA" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        # Run
        TOKEN=$(java -cp "$CLASSPATH:$TEMP_DIR" TokenGen 2>/dev/null)
    fi
fi

# Cleanup
rm -rf "$TEMP_DIR"

# ============================================================================
# Output
# ============================================================================
if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo -e "${YELLOW}Error: Failed to generate token${NC}"
    echo -e "${BLUE}Make sure:${NC}"
    echo "  1. Project is compiled: mvn compile"
    echo "  2. Maven dependencies are downloaded"
    echo "  3. Java is installed and in PATH"
    exit 1
fi

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}JWT Token Generated${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo -e "${BLUE}User ID:${NC} $USER_ID"
echo -e "${BLUE}Email:${NC} $EMAIL"
echo -e "${BLUE}Role:${NC} $ROLE"
echo ""
echo -e "${GREEN}Token:${NC}"
echo "$TOKEN"
echo ""
echo -e "${GREEN}==========================================${NC}"
echo ""
echo -e "${BLUE}Use in curl:${NC}"
echo "Authorization: Bearer $TOKEN"
echo ""
echo -e "${BLUE}Full curl example:${NC}"
echo "curl --location --request PUT 'http://localhost:8081/customers/$USER_ID' \\"
echo "--header 'Authorization: Bearer $TOKEN' \\"
echo "--header 'Content-Type: application/json' \\"
echo "--data-raw '{\"email\": \"$EMAIL\"}'"
echo ""
echo -e "${GREEN}==========================================${NC}"
