# learn-json-parser

A JSON parser written from scratch in Kotlin: a tokenizer, a recursive-descent parser with positioned error messages, and a serializer with compact and pretty output. No parsing libraries.

This repository is a teaching project. The primary artifact is the git history itself: each pull request is a chapter and each commit is a step, meant to be read in order like a guided tutorial.

## What this teaches

- How a tokenizer turns raw text into a typed token stream
- How string escapes and JSON's number grammar are scanned by hand, and where Kotlin's own `toDouble()` and `toIntOrNull()` are too permissive to lean on
- How a recursive-descent parser mirrors the grammar in plain functions, and what that costs (document depth becomes stack depth)
- How errors earn line and column from one grammar fact (no token spans lines) without slowing the happy path
- How a serializer walks a sealed value tree, and why numbers (integral printing, negative zero, non-finite values) are the hard part
- How seeded random round-trip tests check the parser and serializer against each other

## Reading order

The PR chain is stacked: each PR builds on the tip of the previous one, so every PR shows only its own incremental diff. Read PRs in ascending order; within each PR read commits oldest first, and within each commit read the tests before the production code.

| Chapter | Branch | Teaches |
| --- | --- | --- |
| 1 | `milestone-1-tokenizer` | Token stream, structural characters, literals, the error path |
| 2 | `milestone-2-scalars` | String escapes, the exact number grammar |
| 3 | `milestone-3-parser` | Recursive descent, the JsonValue tree |
| 4 | `milestone-4-positioned-errors` | Line and column, PositionedToken |
| 5 | `milestone-5-serializer` | Compact and pretty output, number formatting |
| 6 | `milestone-6-roundtrip` | Seeded generator, round-trip laws |
| 7 | `docs-self-review` | This README, self-critique in the PR description |

## Build and test

```
./gradlew test
```

## Trade-offs

| Decision | Chosen | Alternative | Why |
| --- | --- | --- | --- |
| Tokenization | Eager, into a List | Streaming lexer | Input is already in memory; list buys free lookahead and readable tests |
| Token names | RFC 8259 productions (BeginObject) | Glyph names (LeftBrace) | Parser code reads like the spec |
| End of stream | Explicit EndOfInput token | Nullable peek | One weird token beats null checks in every parse function |
| Number type | Double | BigDecimal / raw text | Matches JSON's JS heritage; loses integers past 2^53 |
| Parser style | Recursive descent | Explicit-stack parser | Code mirrors grammar; costs stack depth on hostile nesting |
| Duplicate keys | Last one wins | Error out | Matches JSON.parse; stricter parsers treat it as an attack shape |
| Error positions | Eager per-token tracking | Lazy recompute from offsets | Tracking is O(1) per newline, laziness would buy nothing |
| Non-finite numbers | Serializer throws | JSON.stringify-style null | Failing fast beats corrupting data silently |
| Escaping | Only what RFC requires | ASCII-only output | Non-ASCII is legal JSON; transport paranoia is opt-in elsewhere |
| Random testing | Hand-rolled seeded generator | Property-testing library | Shows there is no magic; loses shrinking |

## Known limitations

By design, documented where they live in code and in each PR's "deliberately left out" section:

- No depth limit: hostile nesting can overflow the stack
- Lone surrogates in strings pass through unvalidated
- One error per parse, no recovery or error collection
- No streaming input or output; everything is in-memory Strings
- No invalid-input fuzz corpus (e.g. JSONTestSuite); round-trip tests only exercise valid documents

## References

- JSON grammar: https://www.json.org and RFC 8259 (https://datatracker.ietf.org/doc/html/rfc8259)
- Kotlin documentation: https://kotlinlang.org/docs/home.html
- Kotest assertions: https://kotest.io/docs/assertions/assertions.html
- JUnit 5 user guide: https://docs.junit.org/current/user-guide/
- Crockford on JSON's design: https://www.json.org/fatfree.html
- https://github.com/codecrafters-io/build-your-own-x
