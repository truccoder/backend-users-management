# Cache-Aside Pattern Implementation Plan

## Executive Summary

Implement **focused caching** using the Cache-Aside pattern for high-value domains only, leveraging the existing `ReactiveCacheTemplate` from backend-core library.

**Philosophy**: Cache only what provides significant ROI. Avoid caching low-frequency or paginated data where cache hit rates are poor.

---

## Final Cache Architecture

### What We Cache (3 domains)

| Cache | Key Pattern | TTL | Rationale |
|-------|-------------|-----|-----------|
| `user:` | `user:{userId}` | 10m | User profiles - read on every authenticated request |
| `friends:` | `friends:{userId}` | 15m | Friend lists - displayed on main UI, Neo4j query |
| `suggestions:` | `suggestions:{userId}` | 30m | Friend-of-friends - expensive 2-hop Neo4j traversal |

### What We Don't Cache (and why)

| Domain | Reason NOT to cache |
|--------|---------------------|
| `pending-requests` | Short TTL (5m) = low hit rate. Simple PostgreSQL indexed query. |
| `sent-requests` | Same as above. Cache invalidation complexity > benefit. |
| `followers` | Paginated data = each page is separate cache entry = terrible hit rate. |
| `following` | Same pagination anti-pattern. |
| `blocked` | Low access frequency, pagination issue. |

---

## Cache-Aside Pattern

```
READ FLOW:
┌─────────┐    1. get(id)    ┌──────────┐
│ Service │ ───────────────► │  Cache   │
│         │ ◄─────────────── │ (Redis)  │
│         │   2a. HIT        └──────────┘
│         │                        │
│         │   2b. MISS             │
│         │ ◄──────────────────────┘
│         │    3. loadFromDb()
│         │ ───────────────► ┌──────────┐
│         │ ◄─────────────── │ Database │
│         │    4. data       └──────────┘
│         │    5. put(id, data)
│         │ ───────────────► ┌──────────┐
└─────────┘                  │  Cache   │
                             └──────────┘

WRITE FLOW (Invalidation):
┌─────────┐   1. update()   ┌──────────┐
│ Service │ ───────────────►│ Database │
│         │ ◄───────────────│          │
│         │   2. success    └──────────┘
│         │   3. evict(id)
│         │ ───────────────► ┌──────────┐
└─────────┘                  │  Cache   │
                             │ (delete) │
                             └──────────┘
```

---

## Implementation Details

### Files Created

| File | Description |
|------|-------------|
| `config/CacheProperties.java` | Configuration for 3 cache types with TTL and key-prefix |
| `config/CacheConfig.java` | Spring beans for `coreUserCache`, `friendsCache`, `suggestionsCache` |
| `cache/CacheKeyGenerator.java` | Utility for consistent key generation |

### Files Modified

| File | Changes |
|------|---------|
| `services/UserService.java` | Uses `coreUserCache` for user profile caching |
| `services/FriendshipService.java` | Cache-aside for `getFriends()`, `getFriendSuggestions()`. Invalidation on `acceptFriendRequest()`. |
| `application.yaml` | Added `app.caches` configuration |

### Files NOT Modified (intentionally)

| File | Reason |
|------|--------|
| `services/SocialConnectionService.java` | No caching - paginated data has poor hit rate |
| `services/AuthService.java` | No cache invalidation needed |

---

## Configuration

### application.yaml

```yaml
app:
  caches:
    user:
      key-prefix: "user:"
      ttl: 10m
    friends:
      key-prefix: "friends:"
      ttl: 15m
    suggestions:
      key-prefix: "suggestions:"
      ttl: 30m
```

---

## Service Caching Logic

### UserService

```java
public Mono<UserDto> getUserById(Long id) {
  return coreUserCache.get(String.valueOf(id), this::loadUserFromDb);
}

public Mono<Void> evictUserCache(Long id) {
  return coreUserCache.evict(String.valueOf(id));
}
```

### FriendshipService

| Method | Cache Operation |
|--------|-----------------|
| `getFriends(userId)` | Cache-aside: `friendsCache.get(userId, this::loadFriendsFromNeo4j)` |
| `getFriendSuggestions(userId)` | Cache-aside: `suggestionsCache.get(userId, this::loadSuggestionsFromNeo4j)` |
| `acceptFriendRequest()` | **Evict**: both users' friends cache + suggestions cache |
| `getPendingFriendRequests()` | No cache - direct DB query |
| `getSentFriendRequests()` | No cache - direct DB query |

---

## TTL Reasoning

| Cache | TTL | Reasoning |
|-------|-----|-----------|
| `user:` | 10m | Profiles change infrequently. 10m staleness is acceptable. |
| `friends:` | 15m | Friendships are stable once accepted. Longer TTL justified. |
| `suggestions:` | 30m | Expensive computation, changes slowly. Users tolerate stale suggestions. |

**General principles:**
- High write frequency → shorter TTL
- Expensive computation → longer TTL (justify the cache)
- User expectation of real-time → shorter TTL

---

## Verification

### Manual Testing

```bash
# 1. Start local Redis
docker run -d -p 6379:6379 redis:7-alpine

# 2. Run application
./gradlew bootRun --args='--spring.profiles.active=localdev'

# 3. Test friends caching
curl -X GET http://localhost:8090/v1/api/friendships/friends
# First call: cache miss (loads from Neo4j)
# Second call: cache hit

# 4. Verify in Redis CLI
redis-cli
> KEYS friends:*
> TTL friends:1
> GET friends:1

# 5. Test cache invalidation (accept friend request)
curl -X POST http://localhost:8090/v1/api/friendships/requests/1/accept
redis-cli KEYS friends:*  # Both users' caches evicted
```

---

## Trade-offs

| Decision | Rationale |
|----------|-----------|
| **Only 3 caches** | Focus on high-ROI domains. Avoid complexity for marginal gains. |
| **No pagination caching** | Each page = separate cache entry = poor hit rate. Anti-pattern. |
| **Delete-only eviction** | Prevents write-ordering issues in distributed systems. |
| **TTL-based expiry** | Self-healing for stale data without complex invalidation. |
| **Graceful degradation** | Cache failures fall back to DB (built into ReactiveCacheTemplate). |

---

## Future Considerations

If metrics show need for more caching:

1. **Add cache metrics** - Track hit/miss rates before expanding
2. **Consider followers count cache** - Single value, not paginated
3. **Kafka-based invalidation** - For distributed cache consistency
4. **Cache warming** - Pre-populate for hot users on startup
