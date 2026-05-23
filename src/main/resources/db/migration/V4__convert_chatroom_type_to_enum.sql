-- V4: Normalize chat_rooms.type to lowercase values
-- Keep as VARCHAR to maintain compatibility with JPA's @Enumerated(EnumType.STRING)

-- 1) Normalize stored values to lowercase equivalents
UPDATE chat_rooms SET type = LOWER(type) WHERE type IS NOT NULL;

-- 2) Replace any unexpected values with 'builtin' as a safe default
UPDATE chat_rooms SET type = 'builtin' WHERE type NOT IN ('builtin','dm','group');
