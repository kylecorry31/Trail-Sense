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

Issues marked with the help-wanted label are open for community contribution at any time. They are usually low on my list of things to implement, so if you would like to see these in Trail Sense it will be faster to implement them. Please leave a comment on the issue stating that you are working on it or ask for more details (I can also produce AI tool hand-off documents as needed - uses Matt Pocock's grill-me skill, to make sure I get all my ideas written down). If possible, try to give me a timeline so I know which release it should be tagged as. Once you decide to work on one of the help-wanted issues, just submit a PR to main and I will conduct a code review.

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
- Pull requests that have requested changes which haven't received activity (response / commit / etc) after 1 week will be closed due to inactivity. If you are planning on addressing the changes but they are taking a while, please reply to the review comments. If you make changes after the pull request is closed, tag me in a comment or re-open a pull request.

## Survival Guide
The survival guide is heavily curated and all information must be easy to follow and relevant to wilderness survival for the average person. I do not want to include advanced topics or bushcraft in the guide.

I sell a physical copy of the survival guide, so I ask that all content is written by me. I'm open to suggestions for areas to improve or add.

## Generative AI
Code produced by generative AI will be held to the same quality standards as all other code in this repo. The contributor must verify the accuracy and how well it fits into the existing code base (ex. code style, architecture, etc) before requesting a review. Generative AI content has the tendency to be verbose, include unnecessary details, or be incorrect - please thoroughly review and edit any AI generated content **before** submitting it for review. All contributors are expected to be able to answer questions about all of the code/content in their PRs or discussions, regardless of AI use. **Please do not raise a pull request until you fully understand all the code you changed.** Avoid using generative AI for areas that you are not familiar with as it may produce convincing but incorrect results which causes me more work while reviewing.

If you used generative AI for the majority of a pull request, mention which AI tool generated the code in the pull request body so I can tag the squash commit with Assisted-By.

Please do not use AI to generate discussion posts. It is fine to use it to fix your grammar / translate.

The following risk areas provide guidance on which areas AI tooling works well and where it should be avoided by new contributors. You are more likely to have a PR closed for AI implementations of features in the high risk areas as they are harder to meet my quality standards.

### Low risk areas
LLMs perform well on tasks in the low risk category and can typically be done with little to no planning before implementation. These changes can also be easily verified by someone with little to no knowledge of the codebase. Many of these tasks can be done by conventional tools.

- Checking and correcting grammar and spelling
  - Exception: Avoid letting the tool change the actual content
- Translating discussion posts
- Translating in-app and store text
  - Exception: You must be **proficient in both languages** and can verify accuracy
- Fixing linting issues
- Generating scripts 
- Implementing unit tests
  - Exception: Algorithm unit tests must be based on an expected output from a trusted source or calculations done by hand
- Implementing UI automation tests
- Generating boilerplate code
- Upgrading a dependency
- Converting one programming language to another
- Implementing issues that are tagged with "Size: Small"
- Helping put your ideas together (not making the decisions though) - ex. grill-me skill

### Medium risk areas
LLMs may perform decently on tasks in the medium risk category but you may need to spend time doing planning or exploration before having it implement. These changes shouldn't be too difficult to verify if you have little knowledge of the codebase, but plan to spend additional time reviewing and editing before submitting a pull request.

- Implementing issues that are tagged with "Size: Medium"

### High risk areas
LLMs are unlikely to perform well on tasks in the high risk category. You will need to spend significant time planning and exploring before having it implement. You may need to spend significant amounts of time having it fix issues in the implementation or restarting from a fresh slate. The changes will be difficult to verify if you do not have a good knowledge of the codebase. You are likely to have a pull request closed due to quality issues if you use generative AI for tasks in this area. You are likely to use up a lot of tokens unless you have a strong plan created.

- Implementing issues that are tagged with "Size: Large"

### Banned areas
LLM use is not evaluated in the following areas because I do not want generative AI to touch these tasks. Any pull request in this category will likely be closed out unless you can prove AI was not used.

- Generating artwork
- Generating survival guide content
