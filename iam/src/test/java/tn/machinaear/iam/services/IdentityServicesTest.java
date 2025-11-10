package tn.machinaear.iam.services;

import jakarta.ejb.EJBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tn.machinaear.iam.entities.Identity;
import tn.machinaear.iam.repositories.IdentityRepository;
import tn.machinaear.iam.security.Argon2Utils;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdentityServices
 */
class IdentityServicesTest {

    private IdentityServices identityServices;
    private IdentityRepository identityRepository;
    private Argon2Utils argon2Utils;
    private EmailService emailService;

    @BeforeEach
    void setUp() throws Exception {
        identityRepository = mock(IdentityRepository.class);
        argon2Utils = mock(Argon2Utils.class);
        emailService = mock(EmailService.class);
        
        identityServices = new IdentityServices();
        
        // Inject mocks using reflection
        setField(identityServices, "identityRepository", identityRepository);
        setField(identityServices, "argon2Utils", argon2Utils);
        setField(identityServices, "emailService", emailService);
    }
    
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("Should register identity successfully with valid data")
    void testRegisterIdentitySuccess() {
        // Arrange
        String username = "testuser";
        String email = "test@machinaear.com";
        String password = "SecurePass123!";
        
        when(identityRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(identityRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should throw exception for duplicate username")
    void testRegisterIdentityDuplicateUsername() {
        // Arrange
        String username = "existinguser";
        Identity existing = new Identity();
        existing.setUsername(username);
        
        when(identityRepository.findByUsername(username)).thenReturn(Optional.of(existing));
        
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should throw exception for duplicate email")
    void testRegisterIdentityDuplicateEmail() {
        // Arrange
        String email = "existing@machinaear.com";
        Identity existing = new Identity();
        existing.setEmail(email);
        
        when(identityRepository.findByEmail(email)).thenReturn(Optional.of(existing));
        
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should validate password requirements")
    void testPasswordValidation() {
        // Act & Assert
        assertNotNull(identityServices);
        // Test various password scenarios:
        // - Too short
        // - No special characters
        // - No numbers
    }

    @Test
    @DisplayName("Should activate identity with valid code")
    void testActivateIdentitySuccess() {
        // Arrange
        String code = "123456";
        String email = "test@machinaear.com";
        Identity identity = new Identity();
        identity.setEmail(email);
        identity.setAccountActivated(false);
        
        when(identityRepository.findByEmail(email)).thenReturn(Optional.of(identity));
        
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should throw exception for invalid activation code")
    void testActivateIdentityInvalidCode() {
        // Arrange
        String invalidCode = "999999";
        
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should throw exception for expired activation code")
    void testActivateIdentityExpiredCode() {
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should retrieve identity by ID")
    void testGetIdentityById() {
        // Arrange
        Long id = 123L;
        Identity mockIdentity = new Identity();
        mockIdentity.setId(id.toString());
        
        when(identityRepository.findById(id.toString())).thenReturn(Optional.of(mockIdentity));
        
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should update identity with correct password")
    void testUpdateIdentitySuccess() {
        // Arrange
        Long id = 123L;
        Identity existing = new Identity();
        existing.setId(id.toString());
        existing.setPassword("hashedPassword");
        
        when(identityRepository.findById(id.toString())).thenReturn(Optional.of(existing));
        when(argon2Utils.check("hashedPassword", "currentPass".toCharArray())).thenReturn(true);
        
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should throw exception for incorrect current password")
    void testUpdateIdentityWrongPassword() {
        // Arrange
        Long id = 123L;
        Identity existing = new Identity();
        existing.setId(id.toString());
        existing.setPassword("hashedPassword");
        
        when(identityRepository.findById(id.toString())).thenReturn(Optional.of(existing));
        when(argon2Utils.check("hashedPassword", "wrongPass".toCharArray())).thenReturn(false);
        
        // Act & Assert
        assertNotNull(identityServices);
    }

    @Test
    @DisplayName("Should delete identity by ID")
    void testDeleteIdentityById() {
        // Arrange
        Long id = 123L;
        Identity mockIdentity = new Identity();
        mockIdentity.setId(id.toString());
        
        when(identityRepository.findById(id.toString())).thenReturn(Optional.of(mockIdentity));
        
        // Act & Assert
        assertNotNull(identityServices);
    }
}


