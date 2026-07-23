# STANDARDS.md

Project standards and conventions.

## Release notes draft format

Release notes follow a fixed structure so every release reads the same way.
Copy the skeleton below into a new release draft and fill it in; the rules
that follow define how to complete it.

### Structure

```
# vMAJOR.MINOR.PATCH

## Release Notes

### Features

**<Short title of the change>**

<One- or two-sentence summary of the change and why it matters to a user.>

**What's new**
- <New capability, class, recognized input, etc.>

**API changes**
- <Method now declares `throws ...` / new parameter / changed return.>

**Behavior changes**
- <Same API, different result. State the before and after.>

**Migration**
- <What existing callers must do — or note that they compile unchanged.>

**UI**
- <User-facing change to a wizard, dialog, or file filter.>

### Bug Fixes

_None in this release._
```

### Rules

- **Title** is the tag, using the `vMAJOR.MINOR.PATCH` scheme (e.g. `v0.14.1`).
- Top-level sections are always `## Release Notes`, then `### Features` and
  `### Bug Fixes` — in that order.
- Group each notable change under a **bold section title** inside `### Features`
  or `### Bug Fixes`.
- Within a change, use only the sub-headings that apply and drop the rest:
  - **What's new** — new capabilities, classes, or recognized inputs.
  - **API changes** — signature/`throws`/return changes callers must know.
  - **Behavior changes** — same API, different runtime result; state before/after.
  - **Migration** — what existing callers must do to adopt the change.
  - **UI** — user-facing changes to wizards/dialogs/filters.
- Write for a consumer of the library/UI, not the committer. Describe what
  changed and what the reader must do, not the commits that changed it.
- Reference code with backticks: `ClassName`, `methodName(...)`, `.ext`.
- If a category is empty, keep its heading and write `_None in this release._`

## Commit Messages

For commit message format, see [How to Write a Git Commit Message](https://cbea.ms/git-commit/).

Commit message requirements:
1. Separate subject from body with a blank line
2. Limit the subject line to 50 characters
3. Capitalize the subject line
4. Do not end the subject line with a period
5. Do not put issue tags or ticket numbers in the subject line
6. Use the imperative mood in the subject line
7. Do not hard-wrap lines within a paragraph. The body may contain multiple paragraphs, each separated by a blank line; write each paragraph as continuous text with no manual line breaks
8. Use the body to explain what and why vs. how
