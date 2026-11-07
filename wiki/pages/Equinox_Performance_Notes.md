## Significant Dates in History

  - I20061107-0800 - The JARs in the SDK were signed. Previous I-build
    (M3 or I20061102-1715) is unsigned.
  - On or after 20070217 - The updated test framework was used in
    performance tests. The new one doesn't add "-dev bin". (Which is a
    good thing.)
  - ?? - class loader boot delegation was changed

## Notes

  - We need to ensure that JARs are signed (or not signed) in both the
    new tests and the baselines
  - Potential factors in UI Application startup tests
      - In 3.2 the SDK's default perspective was the Resource
        Perspective and in 3.3 it is the Java perspective (Note: This is
        <strong>NOT</strong> true...it was changed earlier. Will leave
        this note so we don't forget and go down this path again)
      - The Welcome screen is also opened in the UI App startup test -
        can we suppress this?

[Category:Equinox](Category:Equinox "wikilink")