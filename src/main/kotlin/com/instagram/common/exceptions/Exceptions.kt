package com.instagram.common.exceptions

/** Thrown when request input fails domain-level validation. */
class BadRequestException(message: String) : RuntimeException(message)

/** Thrown when a JWT is missing or invalid (not authenticated). */
class UnauthorizedException(message: String = "Authentication required") : RuntimeException(message)

/** Thrown when an authenticated user doesn't have permission. */
class ForbiddenException(message: String = "Forbidden") : RuntimeException(message)

/** Thrown when a requested resource doesn't exist. */
class NotFoundException(message: String) : RuntimeException(message)

/** Thrown when an operation violates a uniqueness constraint (e.g. duplicate username). */
class ConflictException(message: String) : RuntimeException(message)
