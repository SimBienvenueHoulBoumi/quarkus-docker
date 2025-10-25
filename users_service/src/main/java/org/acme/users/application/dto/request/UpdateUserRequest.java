package org.acme.users.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Données d'entrée pour la mise à jour du profil utilisateur.
 * Les champs sont optionnels ; les validations ne s'appliquent que
 * lorsqu'une valeur est fournie.
 */
public record UpdateUserRequest(
        @Size(min = 3, max = 64, message = "username must be between 3 and 64 characters")
        String username,

        @Email(message = "email must be valid")
        @Size(max = 128, message = "email must be at most 128 characters")
        String email,

        @Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
        String password
) {
}
