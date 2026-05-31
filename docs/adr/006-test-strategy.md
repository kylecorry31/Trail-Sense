# Test Strategy

Core science, math, and data structures/algorithms code will be extracted to the sol (science and math) and luna (data structures and algorithms) libraries and have unit test coverage.

Important integrations with the Android platform (UI, services, etc) will be tested using automated integration and manual tests rather than unit tests (for the most part). Integration tests should test all flows in the app (where possible), and a subset can be run for automated smoke testing. Unit tests at this level can be done, but the preference is to make the code generic and move to sol/luna for testing.

## Status

accepted

## Consequences

- Test coverage may be suboptimal
- Integration tests take longer to run and require more resources (currently free with GitHub's open source policies)
- Integration tests may not hit all code paths