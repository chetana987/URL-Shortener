# Redis Caching in URL Shortener

## Redis Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Application                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    URL Shortener                         │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │    │
│  │  │ Controller  │→ │  Service    │→ │  Repository     │  │    │
│  │  └─────────────┘  └──────┬──────┘  └────────┬────────┘  │    │
│  │                          │                   │          │    │
│  │                    ┌─────▼─────┐       ┌─────▼─────┐    │    │
│  │                    │ RedisCache│       │   MySQL   │    │    │
│  │                    │  Service  │       │           │    │    │
│  │                    └─────┬─────┘       └─────┬─────┘    │    │
│  └──────────────────────────┼───────────────────┼──────────┘    │
└──────────────────────────────┼───────────────────┼───────────────┘
                               │                   │
                    ┌──────────▼───┐       ┌───────▼───────┐
                    │    Redis     │       │    MySQL     │
                    │   localhost  │       │   localhost  │
                    │     :6379    │       │     :3306    │
                    └──────────────┘       └──────────────┘
```

## Cache-Aside Pattern (Lazy Loading)

```
Request Flow:
┌────────┐    ┌────────────┐    ┌─────────┐    ┌──────────┐
│ Client │ →  │  Service   │ →  │  Redis  │ →  │  MySQL   │
└────────┘    └────────────┘    └─────────┘    └──────────┘
                     │               │               │
                     │  1. Check     │               │
                     │───────────────│               │
                     │               │               │
                     │  2. Cache Hit?│               │
                     │◄──────────────│               │
                     │               │               │
              ┌──────┴──────┐        │               │
              │             │        │               │
           YES│          NO │        │               │
              │             │        │               │
              ▼             ▼        │               │
         Return URL    3. Fetch DB   │               │
              │        ───────────────│───────────────│
              │             │        │               │
              │             ▼        │               │
              │        4. Store Cache│               │
              │        ──────────────│               │
              │             │        │               │
              │             ▼        ▼               │
              │        Return URL ◄───┘               │
              │             │                         │
              ▼             ▼                         ▼
           Done         Done                       Done
```

## Why Caching Improves Performance

### Without Cache (Direct Database):
```
Request: GET /abc123
Time: ~50-100ms per request

Timeline:
┌────────────────────────┐
│ MySQL Query: 50-100ms │
│ TCP Connection: 5ms    │
│ Parse Results: 2ms     │
│ Total: 55-107ms        │
└────────────────────────┘

10,000 requests/min = 10,000 DB queries
```

### With Redis Cache:
```
Request: GET /abc123
Time: ~0.5-2ms per cache hit

Timeline:
┌──────────────────────┐
│ Redis Lookup: 0.5ms  │
│ Deserialize: 0.2ms   │
│ Total: ~0.7ms         │
└──────────────────────┘

10,000 requests/min → 9,500 cache hits, 500 DB queries
```

### Performance Comparison:
```
                    Without Cache    With Cache    Improvement
─────────────────────────────────────────────────────────────────
Avg Response Time      75ms           1.5ms          98%
DB Queries/min         10,000         500            95% reduction
Throughput             1,000 req/s    50,000 req/s   50x increase
```

## Database Load Reduction

```
Before Caching:
┌────────────────────────────────────────────┐
│                                            │
│    Requests    DB Queries    DB Load      │
│    ─────────────────────────────────────  │
│       100/s       100/s          100%     │
│       500/s       500/s          100%     │
│      1000/s      1000/s          100% ⚠️   │
│                                            │
└────────────────────────────────────────────┘

After Caching (90% hit rate):
┌────────────────────────────────────────────┐
│                                            │
│    Requests    DB Queries    DB Load       │
│    ──────────────────────────────────────  │
│       100/s        10/s          10%       │
│       500/s        50/s          50%       │
│      1000/s       100/s          100%      │
│                                            │
└────────────────────────────────────────────┘

Cache hit rate: 90% → 90% fewer DB queries
```

## Cache Key Design

```
Key Pattern: url:{shortCode}
Example: url:abc123DE

Value Structure:
{
  "id": "uuid-123",
  "shortCode": "abc123DE",
  "originalUrl": "https://example.com/very/long/url",
  "expiryDate": "2026-06-11T10:00:00",
  "active": true
}
```

## Cache Expiration Strategy

| Cache Type | TTL | Reason |
|------------|-----|--------|
| URL Mapping | 1 hour | Frequently accessed, rarely changed |
| Redirect URL | 30 min | Fast lookup for redirects |
| Click Stats | 5 min | Real-time-ish, high frequency updates |

## Cache Invalidation

### On URL Update:
```java
// 1. Update database
urlMappingRepository.save(urlMapping);

// 2. Invalidate old cache
redisCacheService.invalidateCache(shortCode);

// 3. Set new cache
redisCacheService.cacheUrlMapping(shortCode, urlMapping);
```

### On URL Delete:
```java
// 1. Invalidate cache first
redisCacheService.invalidateCache(shortCode);

// 2. Then delete from database
urlMappingRepository.delete(urlMapping);
```

### On URL Expiration:
```java
// Scheduled job runs every hour
// Finds expired URLs and invalidates cache
```

## Monitoring & Metrics

```bash
# Check Redis stats
redis-cli INFO stats | grep -E "keyspace|cache"

# Monitor cache hits
redis-cli INFO stats | grep -E "keyspace_hits|keyspace_misses"

# Check memory usage
redis-cli INFO memory
```

## Best Practices

1. **Use short cache keys** - Reduces memory footprint
2. **Set appropriate TTL** - Balance freshness vs performance
3. **Handle cache failures gracefully** - Don't crash if Redis is down
4. **Pre-warm cache on startup** - Load popular URLs
5. **Monitor cache hit rate** - Target >90% for production
6. **Use connection pooling** - Lettuce with connection pool
7. **Serialize efficiently** - Use JSON for Redis objects