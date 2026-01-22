- [Post a feature idea, bug report, or question](https://github.com/kylecorry31/Trail-Sense/discussions/new/choose)
- [Translate Trail Sense](https://github.com/kylecorry31/Trail-Sense/issues/213)
- [Test out nightly builds](https://github.com/kylecorry31/Trail-Sense/discussions/1940)
- [Test out experimental features](https://github.com/kylecorry31/Trail-Sense/discussions/2099)

**Please do not create new issues.** Use [Discussions](https://github.com/kylecorry31/Trail-Sense/discussions/new/choose) for feature ideas, bug reports, and questions. Issues are reserved for maintainers only.

Before posting a new discussion, please search existing [issues](https://github.com/kylecorry31/Trail-Sense/issues) and [discussions](https://github.com/kylecorry31/Trail-Sense/discussions) (both open and closed) to see if your topic has already been addressed.

If you choose to write a new feature yourself, create a new feature discussion post to verify that it is something that I will accept into Trail Sense before your write it (if not, you can always fork this repo and create your own version of Trail Sense). I will conduct a code review on incoming pull requests to verify they align nicely with the rest of the code base and the feature works as intended.

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
- High maintenance burden / likely to cause confusion
- Not technically feasible / will be inaccurate

All code/translation contributors will be credited in the Licenses section of Trail Sense setting (updated weekly).

## Survival Guide
The survival guide is heavily curated and all information must be easy to follow and relevant to wilderness survival for the average person. I do not want to include advanced topics or bushcraft in the guide.

I sell a physical copy of the survival guide, so I ask that all content is written by me. I'm open to suggestions for areas to improve or add.

## Generative AI
Generative AI use is heavily restricted in this repo. It must follow the below rules and always be fully verified by a person (maintainer + contributor). The contributor must verify the accuracy and how well it fits into the existing code base (ex. code style, architecture, etc) before requesting a review.

All contributors are expected to be able to answer questions about all of the code/content in their PRs or discussions (regardless of AI use). If you used generative AI in your contribution, you must disclose it in the PR template.

This policy is in place to ensure the accuracy and reliability of Trail Sense as well as save me time when reviewing contributed discussions or pull requests. It may change as generative AI gets better (although, I personally enjoy writing the code myself) and I will re-evaluate the state of the tools as they improve.

### Low risk areas
Generative AI is allowed for areas that are low risk. Most of this can be done by conventional tools.

- Grammar and spelling checking/correction (with minimal changes to the actual content)
- Low effort linting fixes
- Translation of text in discussions
- Translation of text in the app description and changelogs (as long as the translator is **proficient in both languages** and can verify accuracy)
- Scripts that don't run in production code and don't generate artifacts that are used in production code (ex. experimentation, automation, etc)

### Medium risk areas
Generative AI is allowed for medium risk areas but extra caution should be used.

- Unit tests (algorithm unit tests should be based on an expected output from a trusted source or calculations done by hand)
- UI automation tests
- Writing discussions (any claims/algorithms in the discussion must be confirmed through human research, ideally with sources cited - be careful about AI suggestions in areas you are not familiar with)
- Writing changelogs
- Translation of text content (as long as the translator is **proficient in both languages** and can verify accuracy)
- Scripts for generating models/data for use in the app (accuracy must be verified through tests)
- Boilerplate generation (something that can be easily compared to a reference implementation - typically something you could copy and paste with a few minor changes)
- Autocomplete (as long as it is like conventional autocomplete - autocompletion of an algorithm, function, etc would be considered high risk)
- Conversion of one programming language to another (as long as there are unit tests to verify accuracy). You'll likely have better luck using the "Java to Kotlin" conversion in a JetBrains IDE and then cleaning it up.
- Applying a migration after updating a dependency (if the migration steps are clearly listed by the dependency's author)

Some features may be considered medium risk if they have little impact to the accuracy or reliability of Trail Sense (ask before assuming though).

### High risk areas
Anything not listed above is considered to be high risk. Generative AI is restricted in high risk areas and can only be used if you are very proficient in the area and can fully verify the accuracy of the output.