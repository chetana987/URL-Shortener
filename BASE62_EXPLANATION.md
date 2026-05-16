# Base62 Encoding in URL Shorteners

## What is Base62?

Base62 is a numeral system using 62 characters:
- `0-9` (10 digits)
- `A-Z` (26 uppercase letters)
- `a-z` (26 lowercase letters)

Total: 62 unique characters for encoding.

---

## Why Base62 for URL Shortening?

### 1. Compact Representation
```
Base10 ID:    9999999999      (10 digits)
Base62 ID:    "g5s6RV"        (6 characters)
```
**Result:** 40% shorter URLs!

### 2. URL-Safe Characters
- All Base62 characters are URL-safe
- No special characters like `/`, `?`, `#`
- Works in all browsers and servers

### 3. Human-Readable
- Unlike Base64, no confusing characters like `+`, `/`, `=`
- Easier to type and share
- More aesthetically pleasing

### 4. Predictable Length
```
ID Range          | Base62 Length
------------------|-------------
0 - 61            | 1 char
62 - 3843         | 2 chars
3844 - 238K       | 3 chars
238K - 14.7M      | 4 chars
14.7M - 900B      | 5 chars
```

---

## Base62 vs UUID

| Aspect | Base62 | UUID |
|--------|--------|------|
| **Length** | 6-10 chars | 36 chars (with hyphens) |
| **Readability** | Easy to read/type | Hard to read |
| **URL Safety** | ✅ Always safe | ⚠️ Contains hyphens |
| **Collisions** | Deterministic (based on ID) | Non-deterministic |
| **Storage** | 6-10 bytes | 36 bytes |
| **Sequence** | Sequential | Random |
| **Database Index** | ✅ Excellent | ⚠️ Poor (random inserts) |

### UUID Problems:
```java
// UUID v4: f47ac10b-58cc-4372-a567-0e02b2c3d479
// ❌ 36 characters
// ❌ Random = database page fragmentation
// ❌ Hard to type/remember

// Base62: "g5s6RV"
// ✅ 6 characters
// ✅ Sequential = optimal for database
// ✅ Easy to read/type
```

---

## Time Complexity

### Encoding (Long to Base62)
```
O(k) where k = number of output characters

For long (max ~19 digits):
- Input size: 64 bits
- Output: 6-11 characters
- Complexity: O(11) ≈ O(1) constant time
```

### Decoding (Base62 to Long)
```
O(k) where k = input length

Single pass through string:
- Character lookup: O(1) using index
- Multiplication and addition: O(1)
- Total: O(k) = O(11) ≈ O(1)
```

### Performance Comparison:
```
Operation        | Time Complexity | Notes
-----------------|-----------------|------------------
Base62 encode    | O(n)            | n = output chars
Base62 decode    | O(n)            | n = input chars
MD5 hash         | O(n)            | n = input length
UUID generate    | O(1)            | Uses SecureRandom
```

---

## Algorithm Explained

### Encoding Algorithm:
```java
// Input: 1254376890
// Step 1: Divide by 62 repeatedly
1254376890 / 62 = 20231885 r 20  (index 20 = 'K')
20231885  / 62 = 326320  r 45    (index 45 = 's')
326320    / 62 = 5263   r 14     (index 14 = '2')
5263      / 62 = 84     r 55     (index 55 = 'S')
84        / 62 = 1      r 22     (index 22 = '8')
1         / 62 = 0      r 1      (index 1 = '2')

// Step 2: Reverse remainders
"8S2K s"? - Wait, let me recalculate
```

### Correct Encoding:
```
1254376890 / 62 = 20231885, remainder = 20  -> 'K'
20231885 / 62 = 326320, remainder = 45      -> 's'
326320 / 62 = 5263, remainder = 14           -> '2'
5263 / 62 = 84, remainder = 55              -> 'S'
84 / 62 = 1, remainder = 22                -> '8'
1 / 62 = 0, remainder = 1                  -> '2'

Read reverse: "28S2sK"
```

---

## Implementation Notes

### 1. Character Order
```java
// Index to Character
0 -> '0', 1 -> '1', ... 9 -> '9'
10 -> 'A', 11 -> 'B', ... 35 -> 'Z'
36 -> 'a', 37 -> 'b', ... 61 -> 'z'
```

### 2. Overflow Handling
```java
// long.MAX_VALUE = 9,223,372,036,854,775,807
// Fits in 11 Base62 characters (62^10 > long.MAX)
```

### 3. Performance Optimizations
```java
// Pre-compute character array (avoids charAt)
private static final char[] DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

// Use index lookup instead of indexOf
result = result * BASE + digit;  // O(1) per character
```

---

## Best Practices

1. **Use database ID, not UUID**
   - Deterministic encoding
   - Better database performance

2. **Reserve shorter codes**
   - Pre-allocate 1-6 character codes for premium users

3. **Mix case for uniqueness**
   - `abc123` vs `ABC123` = different codes

4. **Implement cache-friendly decoding**
   - Store decoded ID alongside short code

5. **Handle edge cases**
   - Zero ID: return "0"
   - Negative: throw exception
   - Invalid chars: throw exception