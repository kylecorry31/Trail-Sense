- [Post a feature idea, bug report, or question](https://github.com/kylecorry31/Trail-Sense/discussions/new/choose)
- Translations can be added on Weblate:
  - In app strings: https://hosted.weblate.org/projects/trail-sense/trail-sense-android/
  - Guides: https://hosted.weblate.org/projects/trail-sense/trail-sense-user-guide/
  - Store metadata: https://hosted.weblate.org/projects/trail-sense/trail-sense-store-metadata/
- [Test out nightly builds](https://github.com/kylecorry31/Trail-Sense/discussions/1940)
- [Test out experimental features](https://github.com/kylecorry31/Trail-Sense/discussions/2099)
- [Supported Use Cases](docs/use-cases.md)
- [Architectural Decision Record](docs/adr): records key decisions I've made for Trail Sense's code

## Discussions and Issues
Use [Discussions](https://github.com/kylecorry31/Trail-Sense/discussions/new/choose) for feature ideas, bug reports, and questions. Issues are reserved for maintainers only.

Before posting a new discussion, please search existing [issues](https://github.com/kylecorry31/Trail-Sense/issues) and [discussions](https://github.com/kylecorry31/Trail-Sense/discussions) (both open and closed) to see if your topic has already been addressed.

If you choose to write a new feature yourself, create a new feature discussion post to verify that it is something that I will accept into Trail Sense before your write it. If not, you can always fork this repo and create your own version of Trail Sense.

Issues marked with the help-wanted label are open for community contribution at any time. They are usually low on my list of things to implement, so if you would like to see these in Trail Sense it will be faster to implement them. Please leave a comment on the issue stating that you are working on it or ask for more details (I can produce better requirements - most of the issues are lacking in details because I implement nearly all of them). If possible, try to give me a timeline so I know which release it should be tagged as. Once you decide to work on one of the help-wanted issues, just submit a PR to main and I will conduct a code review.

If you would like to work on an issue that isn't marked with help-wanted, please let me know because it likely does not have that tag for a reason and PRs for it will likely be closed.

If you submit a discussion post, please be civil and constructive - I will consider all feedback, and if I choose not to implement your suggestion I will post my reasoning. If you are experiencing an issue, please include all relevant details to help me understand and reproduce the issue. If you disagree with a change, please describe why you disagree and how I can improve it. If applicable, please provide research / evidence so I can cross verify. Please do not use geneartive AI to write discussion posts; it is fine to use AI to fix your grammar / translate (include your original text as well, I can translate to English on my end).

Common reasons for not being included:

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
- Do not disable linting for your change. Raise with me if you think there's a valid exception.
- You must review your code before raising the pull request.
- You must understand the code you are submitting for review and be able to answer questions about the code.
- If your pull request contains a portion of the issue, explicitly call out what was changed and what is remaining.
- If you spent time designing the feature before implementing, feel free to post the design documents in the PR description. Or even better, post it in the related issue before making changes if you want a maintainer to review.
- If your feature involves a change to how the app is used, document it in the user guide. See `.agents/skills/trail-sense-user-guides` for more info. If you are unsure, comment on the issue. If you would like a maintainer to write this, please note that in your PR description.
- Most features that have a user guide change will also need an automated test change. See `.agents/skills/trail-sense-android-tests` for more info. If you are unsure, comment on the issue.
- Unit tests are not required for UI or infrastructure changes, but should be added if you are writing domain logic (most logic lives in the `kylecorry31/sol` repo). If you are unsure, comment on the issue.
- Pull requests that have requested changes which haven't received activity (response / commit / etc) after 1 week will be closed due to inactivity. If you are planning on addressing the changes but they are taking a while, please reply to the review comments. If you make changes after the pull request is closed, tag me in a comment or re-open a pull request.
- If you are new to Trail Sense developement, I recommend starting with small sized issues.
- I don't care about commit messages, I squash merge with my own message when I complete your PR.

## Survival Guide
The survival guide is heavily curated and all information must be easy to follow and relevant to wilderness survival for the average person. I do not want to include advanced topics or bushcraft in the guide.

I sell a physical copy of the survival guide, so I ask that all content is written by me. I'm open to suggestions for areas to improve or add.

## Generative AI
I don't care what tools you use*. Review and understand the output, clean it up before sending it my way, and don't submit vibecoded code or AI generated discussions.

*The following uses are banned in Trail Sense:
- Generating artwork
- Generating survival guide content (survival guide is locked for contributions anyway)
- Generating translations in a language you are not proficient in
