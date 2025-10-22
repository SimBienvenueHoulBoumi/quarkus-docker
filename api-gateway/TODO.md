# API Gateway Registration Fix - TODO

## Tasks to Complete:

- [x] Create DTO classes in api-gateway
  - [x] Create RegisterRequest.java
  - [x] Create LoginRequest.java
  - [x] Create AuthResponse.java
  - [x] Create UserResponse.java

- [x] Update UsersServiceClient.java
  - [x] Replace Object types with proper DTOs
  - [x] Remove @Transactional annotation
  - [x] Fix method signatures

- [x] Update UsersGatewayResource.java
  - [x] Replace Object types with proper DTOs
  - [x] Add @Valid annotations
  - [x] Fix method signatures

- [ ] Testing
  - [ ] Test registration endpoint
  - [ ] Verify error handling
  - [ ] Confirm JWT token response

## Summary of Changes:

### Created Files:
1. `src/main/java/org/acme/gateway/dto/RegisterRequest.java` - Registration request DTO with validation
2. `src/main/java/org/acme/gateway/dto/LoginRequest.java` - Login request DTO with validation
3. `src/main/java/org/acme/gateway/dto/AuthResponse.java` - Authentication response DTO
4. `src/main/java/org/acme/gateway/dto/UserResponse.java` - User response DTO

### Modified Files:
1. `src/main/java/org/acme/gateway/client/UsersServiceClient.java`
   - Replaced all `Object` types with proper DTOs
   - Added proper return types (AuthResponse, UserResponse, List<UserResponse>)
   - Removed incorrect @Transactional annotation

2. `src/main/java/org/acme/gateway/resource/UsersGatewayResource.java`
   - Replaced all `Object` types with proper DTOs
   - Added @Valid annotations for request validation
   - Added proper return types

## Next Steps:
1. Rebuild the api-gateway service
2. Test the registration endpoint: POST /api/auth/register
3. Verify proper error messages for validation failures
4. Confirm JWT token is returned correctly
