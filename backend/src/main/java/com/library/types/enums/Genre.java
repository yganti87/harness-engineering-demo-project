package com.library.types.enums;

/**
 * Book genre categories.
 *
 * <p>Values must match the CHECK constraint in V1__create_library_schema.sql.
 * When adding a new genre, also add a new Flyway migration to update the constraint.
 */
public enum Genre {
    FICTION,
    NON_FICTION,
    TECHNOLOGY,
    SCIENCE,
    HISTORY,
    BIOGRAPHY,
    MYSTERY,
    ROMANCE,
    FANTASY,
    SCIENCE_FICTION,
    SELF_HELP,
    BUSINESS,
    OTHER
}
