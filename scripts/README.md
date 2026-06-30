# Docs Utilities

## TOC / Heading Checker

Validate that Markdown TOC anchors in key docs exist in actual headings.

Checks:

- `docs/guides/002-user-guide.md`
- `docs/guides/001-installation-guide.md`

Run from repository root:

```bash
npm run docs:check
```

Expected output:

- `status: OK` when all TOC links are valid
- `status: FAIL` plus line numbers for broken anchors

