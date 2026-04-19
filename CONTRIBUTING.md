- [Post a feature idea, bug report, or question](https://github.com/kylecorry31/Trail-Sense/discussions/new/choose)
- Translations can be added on Weblate:
  - In app strings: https://hosted.weblate.org/projects/trail-sense/trail-sense-android/
  - Guides: https://hosted.weblate.org/projects/trail-sense/trail-sense-user-guide/
  - Store metadata: https://hosted.weblate.org/projects/trail-sense/trail-sense-store-metadata/
- [Test out nightly builds](https://github.com/kylecorry31/Trail-Sense/discussions/1940)
- [Test out experimental features](https://github.com/kylecorry31/Trail-Sense/discussions/2099)

Use [Discussions](https://github.com/kylecorry31/Trail-Sense/discussions/new/choose) for feature ideas, bug reports, and questions. Issues are reserved for maintainers only.

Before posting a new discussion, please search existing [issues](https://github.com/kylecorry31/Trail-Sense/issues) and [discussions](https://github.com/kylecorry31/Trail-Sense/discussions) (both open and closed) to see if your topic has already been addressed.

If you choose to write a new feature yourself, create a new feature discussion post to verify that it is something that I will accept into Trail Sense before your write it (if not, you can always fork this repo and create your own version of Trail Sense).

Issues marked with the help-wanted label are open for community contribution at any time. They are usually low on my list of things to implement, so if you would like to see these in Trail Sense it will be faster to implement them. Please leave a comment on the issue stating that you are working on it or ask for more details. If possible, try to give me a timeline so I know which release it should be tagged as. Once you decide to work on one of the help-wanted issues, just submit a PR to main and I will conduct a code review.

If you would like to work on an issue that isn't marked with help-wanted, please let me know because it may not have all the details, I may not want it implemented yet, or I may want to implement it myself. All issues are correctly tagged with help-wanted.

If you submit a discussion post, please be civil and constructive - I will consider all feedback, and if I choose not to implement your suggestion I will post my reasoning. If you are experiencing an issue, please include all relevant details to help me understand and reproduce the issue. If you disagree with a change, please describe why you disagree and how I can improve it. Finally, if applicable, please provide research / evidence so I can cross verify. Common reasons for not being included:

- Doesn't fit into Trail Sense's use cases
- Frequency of use (ex. edge case) vs effort to implement 
- Not backed by science/research
- Requires a lot of effort to use (external data from a website, many steps, external tools, etc.)
- Dangerous (to the user or their device)
- Requires Internet (may be better as a plugin)
- Significantly increases APK size (definition of significant varies by feature usefulness - APK should be under 10 MB)
- Clutters the UI
- High maintenance burden or likely to cause confusion
- Not technically feasible or will be inaccurate

All code/translation contributors will be credited in the Licenses section of Trail Sense setting (updated weekly).

## Pull Requests
- All sections of the pull request description template must be filled out.
- There must be a related issue and your code must implement it as specified. Raise in discussions or the issue if you believe something different is needed before submitting the pull request for review.
- If your change involves modifications to the app code (ex. not just tests/docs/etc), a screenshot is required.
- Only include changes related to the issue you are addressing. For example, don't adjust the code styling of the file.
- Don't include in code comments that describe what you changed. You can mention things via comments in the GitHub pull request interface or in the PR description.
- You must attempt to follow the coding style of this project.
- Your PR must pass the build validation (test + detekt).
- Do not disable linting for your change.
- You are required to review your code before raising the pull request.
- If you used AI tooling to generate the code, you need to review and clean it up before raising.
- You must abide by the "Generative AI" policy below if you used generative AI tooling.
- If your pull request contains a portion of the issue, explicitly call out what was changed and what is remaining.
- If you spent time designing the feature before implementing, feel free to post the design documents in the PR description. Or even better, post it in the related issue before making changes if you want a maintainer to review.
- If your feature involves a change to how the app is used, document it in the user guide. See `.agents/skills/trail-sense-user-guides` for more info. If you are unsure, comment on the issue. If you would like a maintainer to write this, please note that in your PR description.
- Most features that have a user guide change will also need an automated test change. See `.agents/skills/trail-sense-android-tests` for more info. If you are unsure, comment on the issue.
- Unit tests are not required for UI or infrastructure changes, but should be added if you are writing domain logic (most logic lives in the `kylecorry31/sol` repo). If you are unsure, comment on the issue.

## Survival Guide
The survival guide is heavily curated and all information must be easy to follow and relevant to wilderness survival for the average person. I do not want to include advanced topics or bushcraft in the guide.

I sell a physical copy of the survival guide, so I ask that all content is written by me. I'm open to suggestions for areas to improve or add.

## Generative AI
Generative AI use is allowed in this repo with extreme caution. It must follow the below rules and always be fully verified by a person (maintainer + contributor). The contributor must verify the accuracy and how well it fits into the existing code base (ex. code style, architecture, etc) before requesting a review. Generative AI content has the tendency to be verbose, include unnecessary details, or be incorrect - please thoroughly review and edit any AI generated content **before** submitting it for review.

All contributors are expected to be able to answer questions about all of the code/content in their PRs or discussions (regardless of AI use). Avoid using generative AI for areas that you are not familiar with as it may produce convincing but incorrect results (which causes me more work while reviewing). Do not "vibe code" Trail Sense changes.

This policy is in place to ensure the accuracy and reliability of Trail Sense as well as save me time when reviewing contributed discussions or pull requests. I review everything the same regardless, but I don't want to waste time if the author isn't putting in the time to review and clean up AI generated code.

I recommend you use the available skills in `.agents/skills` or design/plan the change first and then walk the agent through it step by step to ensure it is high quality.

### Low risk areas
Generative AI is allowed for areas that are low risk. Most of this can be done by conventional tools.

- Grammar and spelling checking/correction (with minimal changes to the actual content)
- Low effort linting fixes
- Translation of text in discussions
- Translation of text in the app description and changelogs (as long as the translator is **proficient in both languages** and can verify accuracy)
- Scripts that don't run in production code and don't generate artifacts that are used in production code (ex. experimentation, automation, etc)
- Unit tests (algorithm unit tests should be based on an expected output from a trusted source or calculations done by hand)
- UI automation tests
- Writing discussions (any claims/algorithms in the discussion must be confirmed through human research, ideally with sources cited - be careful about AI suggestions in areas you are not familiar with)
- Writing changelogs

### Medium risk areas
Generative AI is allowed for medium risk areas but extra caution should be used.

- Translation of text content (as long as the translator is **proficient in both languages** and can verify accuracy)
- Scripts for generating models/data for use in the app (accuracy must be verified through tests)
- Boilerplate generation (something that can be easily compared to a reference implementation - typically something you could copy and paste with a few minor changes)
- Conversion of one programming language to another (as long as there are unit tests to verify accuracy). You'll likely have better luck using the "Java to Kotlin" conversion in a JetBrains IDE and then cleaning it up.
- Applying a migration after updating a dependency (if the migration steps are clearly listed by the dependency's author)

Some features may be considered medium risk if they have little impact to the accuracy or reliability of Trail Sense (ask before assuming though).

### High risk areas
Anything not listed above is considered to be high risk. Generative AI is restricted in high risk areas and can only be used if you are very proficient in the area and can fully verify the accuracy of the output.

Artwork and survival guide content **must be created by a human only**, other than minor grammar and spelling correction.
