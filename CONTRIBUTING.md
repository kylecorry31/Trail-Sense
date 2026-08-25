# Trail Sense Contributing Policy
In general, please be civil and constructive. Do not use generative AI to communicate in this repo (ex. discussion posts, comments, etc.); it is fine to use AI to fix your grammar / translate (include your original text as well, I can translate to English on my end).

Issues, pull requests, or discussions with unanswered questions/comments by a maintainer that haven't received a response (response / commit / etc) after 1 week will be closed due to inactivity. If you claimed an issue and haven't given an update after 1 week, it will be unassigned so it is open to other contributors again. If you are planning on addressing the changes but they are taking a while, please reply so I know you are working on it.

NOTE: Trail Sense is maintained by just me, so please make sure to follow this policy to make my job easier.

## Request a Feature
Use [Discussions](https://github.com/kylecorry31/Trail-Sense/discussions/new/choose) to raise feature requests. If you are looking to recommend another app, you can do so [here](https://github.com/kylecorry31/Trail-Sense/discussions/4002).

Before posting a new discussion, please search existing [issues](https://github.com/kylecorry31/Trail-Sense/issues) and [discussions](https://github.com/kylecorry31/Trail-Sense/discussions) (both open and closed) to see if your feature request has already been raised.

If you choose to write a new feature yourself, create a new feature discussion post to verify that it is something that I will accept into Trail Sense before you write it. If not, you can always fork this repo and create your own version of Trail Sense.

I will consider all requests, and if I choose not to implement your suggestion I will post my reasoning. If you disagree with a change, please describe why you disagree and how I can improve it. If applicable, please provide research / evidence so I can cross verify. Here are some common reasons for a feature not being included:

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

You can view the [use cases](docs/use-cases.md) document to get a sense of where the boundaries are for what Trail Sense will support.

## Report a Bug or Ask a Question
Use [Discussions](https://github.com/kylecorry31/Trail-Sense/discussions/new/choose) to report bugs or ask questions.

Before creating a new discussion, please search existing [issues](https://github.com/kylecorry31/Trail-Sense/issues) and [discussions](https://github.com/kylecorry31/Trail-Sense/discussions) (both open and closed) to see if your issue has already been addressed.

If you submit a bug report or question, please be civil and constructive - I will consider all feedback. Please include all relevant details to help me understand and reproduce the issue. Do not use generative AI to write discussion posts; it is fine to use AI to fix your grammar / translate (include your original text as well, I can translate to English on my end).

## Contribute a Code Change
Issues marked with the `Status: Community Available` label are open for community contribution at any time. Please leave a comment on the issue stating that you are working on it or ask for more details (I can write better requirements if needed). If possible, try to give me a timeline so I know which release it should be tagged as. Once you decide to work on one of the `Status: Community Available` issues, just submit a PR to main and I will conduct a code review. If you would like to work on an issue that isn't marked with `Status: Community Available`, please let me know because it does not have that tag for a reason and PRs for it will likely be closed. If you are new to Trail Sense development, I recommend starting with a `Status: Community Available` issue that is also tagged with `Size: Small`.

All code/translation contributors will be credited in the Licenses section of Trail Sense settings (updated weekly).

See the [Architectural Decision Record](docs/adr) for key decisions I've made for Trail Sense's code.

### Pull Request Policy
- You are required to review your pull request first and then leave a **comment on the pull request** stating something along the lines of "I have reviewed my pull request, it is ready for additional review" before it will be reviewed by a maintainer.
- You must understand the code you are submitting for review and be able to answer questions about the code.
- All sections of the pull request description template must be filled out before it will be reviewed.
- Use the PR description to describe what this change does and why (not how). Do not generate it with AI.
- If your pull request contains a portion of the issue, explicitly call out what was changed and what is remaining (in the PR description).
- If your change involves modifications to the app code (ex. not just tests/docs/etc), a screenshot is required.
  - You can use an emulator or disable auto-update location in Settings > Sensors > GPS to pick a fake location if the screenshot would include your location.
- The automated PR checks must pass before it will be reviewed.
- Do not disable linting for your change. Raise with me if you think there's a valid exception.
- Only include changes related to the issue you are addressing. For example, don't adjust the code styling of the file.
- Don't include in code comments that describe what you changed. You can mention things via comments in the GitHub pull request interface or in the PR description.
- You must attempt to follow the coding style of this project.
- If your feature involves a change to how the app is used, document it in the user guide. If you are unsure, comment on the issue. If you would like a maintainer to write this, please note that in your PR description.
- Most features that have a user guide change will also need an automated test change. See `.agents/skills/trail-sense-android-tests` for more info. If you are unsure, comment on the issue.
- Unit tests are not required for UI or infrastructure changes, but should be added if you are writing domain logic. If you are unsure, comment on the issue.
- I don't care about commit messages, I squash merge with my own message when I complete your PR.
- I don't care if you use generative AI, but you are required to review and understand its output and clean it up before raising a PR. Generating code without reviewing it ("vibecoding") and generating image assets are banned.

Pull requests that appear to be unreviewed AI generated output will be closed without review.

## Translate
Translations can be added on Weblate:

- In app strings: https://hosted.weblate.org/projects/trail-sense/trail-sense-android/
- Guides: https://hosted.weblate.org/projects/trail-sense/trail-sense-user-guide/
- Store metadata: https://hosted.weblate.org/projects/trail-sense/trail-sense-store-metadata/

### Contributing Policy
- Do not use machine translation unless you are proficient in both English and the language you are translating to.
- If you use machine translation, you need to fully proofread it and confirm it is an accurate translation.
- If you are suspected of using machine translations without proofreading or are submitting a large number of incorrect translations, you will be banned from translations and need to reach out to me to discuss details about me unbanning you.
- Do not use machine translation to translate the survival guide.
- Do not add or remove content, or otherwise change the meaning of the translated text.
- For the field guides, it is fine to swap it for the equivalent article in the language you are translating to, as long as that article is about the same subject as the English one and is at the same taxonomic rank (ex. if the English link is for a genus, don't link to a single species or to the whole family). If there is no equivalent article at the same rank, keep the English link.

Machine translation = translating using a computer, whether that is something like Google Translate or AI it falls into the same bucket.

All translation contributors will be credited in the Licenses section of Trail Sense settings (updated weekly).

## Test
You can contribute to Trail Sense by testing to find bugs or identify areas it can be improved. Use the discussions to post issues that you find.

You can help with testing for the unreleased version of Trail Sense by using the [nightly builds](https://github.com/kylecorry31/Trail-Sense/discussions/1940) or by enabling and using [experimental features](https://github.com/kylecorry31/Trail-Sense/discussions/2099).

If you use generative AI to automatically identify bugs, please make sure you manually verify before creating a discussion about it.

## Survival Guide
The survival guide is heavily curated and all information must be accurate, easy to follow, and relevant to wilderness survival for the average person. I do not want to include advanced topics or bushcraft in the guide.

It is not open to contributions other than translations but I'm open to suggestions for areas to improve or add. Do not translate this guide with machine translation.

## Violations
Repeated violations of the contributing policy will result in a contribution ban. The ban will be fine-grained if possible (ex. just pull requests, weblate, etc.).