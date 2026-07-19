# learn-json-parser

A JSON parser written from scratch in Kotlin: a tokenizer, a recursive-descent parser with positioned error messages, and a serializer with compact and pretty output. No parsing libraries.

This repository is a teaching project. The primary artifact is the git history itself: each pull request is a chapter and each commit is a step, meant to be read in order like a guided tutorial.

## What this teaches

- How a tokenizer turns raw text into a typed token stream
- How string escapes and JSON's number grammar are scanned by hand
- How a recursive-descent parser mirrors the grammar in plain functions
- How parse errors carry line and column so failures point at the input
- How a serializer walks a value tree, in compact and pretty form
- How seeded random round-trip tests catch parser/serializer asymmetries

## Reading order

The PR chain is stacked: each PR builds on the tip of the previous one, so every PR shows only its own incremental diff. Read PRs in ascending order, and within each PR read commits oldest first. The final docs PR completes this section with the full chapter list.

## Build and test

```
./gradlew test
```

## Trade-offs

The trade-offs table is completed in the final docs PR, after the decisions it summarizes exist in the history.

## References

- JSON grammar: https://www.json.org and RFC 8259
- Kotlin documentation: https://kotlinlang.org/docs/home.html
- Kotest assertions: https://kotest.io/docs/assertions/assertions.html
- JUnit 5 user guide: https://docs.junit.org/current/user-guide/
- https://github.com/codecrafters-io/build-your-own-x
