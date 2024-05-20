# Contributing to Dogcat

Are you new to Open Source?

If you’re a new open source contributor, the process can be intimidating.
What if you don’t know how to code? What if something goes wrong? Don't worry!

You don’t have to contribute code! A common misconception about contributing to open source is that you need to
contribute code. In fact, it’s often the other parts of a project that are most neglected or overlooked. You’ll do the
project a huge favor by offering to pitch in with these types of contributions!

Even if you like to write code, other types of contributions are a great way to get involved with a project and meet
other community members. Building those relationships will give you opportunities to work on other parts of the project.

We would love for you to contribute to Angular and help make it even better than it is
today! As a contributor, here are the guidelines we would like you to follow:

- [Code of Conduct](#coc)
- [Question or Problem?](#question)
- [Issues and Bugs](#issue)
- [Feature Requests](#feature)
- [Submission Guidelines](#submit)
- [Coding Rules](#rules)
- [Commit Message Guidelines](#commit)
- [Signing the CLA](#cla)

## <a name="coc"></a> Code of Conduct

Help us keep Angular open and inclusive. Please read and follow our [Code of Conduct][coc].

## <a name="question"></a> Got a Question or Problem?

Do not open issues for general support questions as we want to keep GitHub issues for bug reports and feature requests.
You've got much better chances of getting your question answered
on [Stack Overflow](https://stackoverflow.com/questions/tagged/angular) where the questions should be tagged with
tag `angular`.

Stack Overflow is a much better place to ask questions since:

- there are thousands of people willing to help on Stack Overflow
- questions and answers stay available for public viewing so your question / answer might help someone else
- Stack Overflow's voting system assures that the best answers are prominently visible.

To save your and our time, we will systematically close all issues that are requests for general support and redirect
people to Stack Overflow.

If you would like to chat about the question in real-time, you can reach out via [our gitter channel][gitter].

## <a name="issue"></a> Found a Bug?

If you find a bug in the source code, you can help us by
[submitting an issue](#submit-issue) to our [GitHub Repository][github]. Even better, you can
[submit a Pull Request](#submit-pr) with a fix.

## <a name="feature"></a> Missing a Feature?

You can *request* a new feature by [submitting an issue](#submit-issue) to our GitHub
Repository. If you would like to *implement* a new feature, please submit an issue with
a proposal for your work first, to be sure that we can use it.
Please consider what kind of change it is:

* For a **Major Feature**, first open an issue and outline your proposal so that it can be
  discussed. This will also allow us to better coordinate our efforts, prevent duplication of work,
  and help you to craft the change so that it is successfully accepted into the project.
* **Small Features** can be crafted and directly [submitted as a Pull Request](#submit-pr).

Help wanted! We'd love your contributions to Act. Please review the following guidelines before contributing. Also, feel
free to propose changes to these guidelines by updating this file and submitting a pull request.

- [I have a question...](#questions)
- [I found a bug...](#bugs)
- [I have a feature request...](#features)
- [I have a contribution to share...](#process)

## <a id="questions"></a> Have a Question?

Please don't open a GitHub issue for questions about how to use `act`, as the goal is to use issues for managing bugs
and feature requests. Issues that are related to general support will be closed and redirected to our gitter room.

For all support related questions, please ask the question in our gitter
room: [nektos/act](https://gitter.im/nektos/act).

## <a id="bugs"></a> Found a Bug?

If you've identified a bug in `act`, please [submit an issue](#issue) to our GitHub
repo: [nektos/act](https://github.com/nektos/act/issues/new). Please also feel free to submit a [Pull Request](#pr) with
a fix for the bug!

## <a id="features"></a> Have a Feature Request?

All feature requests should start with [submitting an issue](#issue) documenting the user story and acceptance criteria.
Again, feel free to submit a [Pull Request](#pr) with a proposed implementation of the feature.

## <a id="process"></a> Ready to Contribute

### <a id="issue"></a> Create an issue

Before submitting a new issue, please search the issues to make sure there isn't a similar issue doesn't already exist.

Assuming no existing issues exist, please ensure you include required information when submitting the issue to ensure we
can quickly reproduce your issue.

We may have additional questions and will communicate through the GitHub issue, so please respond back to our questions
to help reproduce and resolve the issue as quickly as possible.

New issues can be created with in our [GitHub repo](https://github.com/nektos/act/issues/new).

PRs are always welcomed from everyone. Following this guide will help us get your PR reviewed and merged as quickly as
possible.

This project has adopted the code of conduct defined by the Contributor Covenant to clarify expected behavior in our
community.
For more information see the [Contributor Covenant Code of Conduct](https://dotnetfoundation.org/code-of-conduct)

We would love to accept your patches and contributions to this project.
We're thrilled you're considering participating in this project. We aim to make this process as easy and transparent as
possible. Here are some guidelines to help you get started.

## Quicklinks

- [Getting Started](#getting-started)
    - [Issues](#issues)
    - [Pull Requests](#pull-requests)
    - [Reviews](#reviews)
    - [Documentation](#documentation)
- [Getting Help](#getting-help)

## Ways to Contribute

Any help in any form is appreciated, here are the funnels:

- Spread the word about this project in your social networks if you find this project interesting
- Ask a question, leave a comment or feedback at [Discussions]
- File a bug or feature request at [Issues]
    - There are templates for your convenience
- Implement a feature or fix a bug and submit a [PR]

## Before You Begin

If you're planning a significant change, we recommend starting a discussion as a GitHub Issue first. This allows the
community and maintainers to provide guidance and coordinate efforts, preventing wasted time or overlap.

Please be aware this project adopts Code Of Conduct, if your contributions are constructive and positive, no need to get
into too many details of it.

One subtle but important thing, if you decide to contribute, please make sure to sign off your commits.
By doing so, you sign DCO, which in short means "my contribution belongs to me, I have a right to make it, I transfer my
contribution under this project's license".
This practice helps to avoid possible legal issues. Yes, the project is far from those, but it's good to start doing the
right thing from the beginning.
You only need to refer to the DCO section if contributing to source.

## Pull Requests

Good pull requests—patches, improvements, new features—are a fantastic
help. They should remain focused in scope and avoid containing unrelated
commits.

**Please ask first** before embarking on any **significant** pull request (e.g.
implementing features, refactoring code, porting to a different language),
otherwise you risk spending a lot of time working on something that the
project's developers might not want to merge into the project. For trivial
things, or things that don't require a lot of your time, you can go ahead and
make a PR.

We actively welcome your pull requests. Here's the process:

1. [Fork](https://help.github.com/articles/fork-a-repo/) the project This is a standard part of all open-source
   projects. You'll need your copy of the codebase
   to make changes.
   , clone your fork,
   and configure the remotes:

   ```bash
   # Clone your fork of the repo into the current directory
   git clone https://github.com/<your-username>/bootstrap.git
   # Navigate to the newly cloned directory
   cd bootstrap
   # Assign the original repo to a remote called "upstream"
   git remote add upstream https://github.com/twbs/bootstrap.git
   ```

2. If you cloned a while ago, get the latest changes from upstream:

   ```bash
   git checkout main
   git pull upstream main
   ```

3. Create a new topic branch (off the main project development branch) to
   contain your feature, change, or fix:     Make a new branch from `main` in your fork. This keeps your changes
   separate and organized.

   ```bash
   git checkout -b <topic-branch-name>
   ```

4. Implement your featuer and add tests. If you've added code that should be tested, please also write the tests that
   verify its functionality.Commit your changes in logical chunks. Please adhere to these [git commit
   message guidelines](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html)
   or your code is unlikely be merged into the main project. Use Git's
   [interactive rebase](https://help.github.com/articles/about-git-rebase/)
   feature to tidy up your commits before making them public.

5. Pass all tests
   Ensure the test suite passes. This shows that your changes don't break existing functionality.

6. **Lint Your Code**: Make sure your code adheres to our linting standards. This ensures code quality and consistency.


4. **Update Documentation**: If you've changed APIs or added features, update the relevant documentation to reflect
   this.

5. Locally merge (or rebase) the upstream development branch into your topic branch:

   ```bash
   git pull [--rebase] upstream main
   ```

6. Push your topic branch up to your fork:

   ```bash
   git push origin <topic-branch-name>
   ```

7. [Open a Pull Request](https://help.github.com/articles/about-pull-requests/)
   with a clear title and description against the `main` branch.
   Submit your changes as a pull request (PR) to the main repository.
8. ## Code reviews

All submissions, including submissions by project members, require review. We
use GitHub pull requests for this purpose. Consult
[GitHub Help](https://help.github.com/articles/about-pull-requests/) for more
information on using pull requests.

## License

## Appendix A: Building the project

#### Preconditions

You need following software on your machine in order to start developing:

* Java 17+
  Installed JDK plus JAVA_HOME environment variable set
  up and pointing to your Java installation directory. Used to compile and build the Citrus code.
* Java IDE (optional)
  A Java IDE will help you to manage your Citrus project (e.g. creating
  and executing test cases). You can use the Java IDE that you like best like Eclipse or IntelliJ IDEA.

### Editor

Please use an editor with support for [ESLint](http://eslint.org/) and [EditorConfig](http://editorconfig.org/).
Configuration
files for both tools are provided in the root directory of the project.

#### Importing into IntelliJ IDEA

To import into IntelliJ IDEA, just open up the `Ktor` project folder. IntelliJ IDEA should automatically detect
that it is a Gradle project and import it. It's important that you make sure that all building and test operations
are delegated to Gradle under [Gradle Settings](https://www.jetbrains.com/help/idea/gradle-settings.html).

## Appendix B: Code style and coding conventions

### Style guide

Changes must adhere to the style guide and this will be verified by the continuous integration build.

* Java code style is [Google style](https://google.github.io/styleguide/javaguide.html).
* Kotlin code style is [ktfmt w/ Google style](https://github.com/facebookincubator/ktfmt#ktfmt-vs-ktlint-vs-intellij).
* Scala code style is [scalafmt](https://scalameta.org/scalafmt/).
* Python adheres to the pep8 standard.

Java and Scala code style is checked by [Spotless](https://github.com/diffplug/spotless)
with [google-java-format](https://github.com/google/google-java-format) and
[scalafmt](https://scalameta.org/scalafmt/) during build.

Python code style is checked by flake8/black.

#### Automatically fixing code style issues

Java, Scala and Kotlin code style issues can be fixed from the command line using
`./gradlew spotlessApply`.

### Coding conventions

We follow the [Kotlin Coding Conventions](http://kotlinlang.org/docs/reference/coding-conventions.html)

## Appendix 1: Developer Certificate of Origin (DCO)

“I have the right to submit it under the open source license indicated in the file”

By making a contribution to this project, you certify that:

- The contribution was created in whole or in part by you and you have the right to submit it under the open source
  license indicated in the file; or
- The contribution is based upon previous work that, to the best of your knowledge, is covered under an appropriate open
  source license and you have the right under that license to submit that work with modifications, whether created in
  whole or in part by you, under the same open source license (unless you are permitted to submit under a different
  license), as indicated in the file; or
- The contribution was provided directly to you by some other person who certified (a), (b) or (c) and you have not
  modified it.

You understand and agree that this project and the contribution are public and that a record of the contribution (
including all personal information you submit with it, including your sign-off) is maintained indefinitely and may be
redistributed consistent with this project or the open source license(s) involved.

For more information, see the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).

The Developer Certificate of Origin (DCO)(insert link) is a legal statement that a contributor makes to certify that
they are the original creator of their contribution, and that they have the right to submit it under the open source
license used by the project.

The reason a project asks contributors to sign a DCO is to avoid legal issues down the line. It helps ensure that all
contributors are aware of the license terms and agree to them, and that they are not contributing someone else's
copyrighted work without permission. This protects the project, its users, and its contributors.

To sign the DCO, you add a line to every git commit message:

You can automate this with git by using a command like git commit -s.

In order to accept your pull request, we need you to submit a DCO. You only need to do this once to work on any of our
open source projects.

To sign your commit, just add a line at the end of your commit message:
Signed-off-by: Jane Smith jane.smith@email.com

Replace `jane.smith@email.com` with your correct email.

**IMPORTANT**: By submitting a patch, you agree to allow the project owners to
license your work under the terms of the [MIT License](../LICENSE) (if it
includes code changes) and under the terms of the
[Creative Commons Attribution 3.0 Unported License](https://creativecommons.org/licenses/by/3.0/)
(if it includes documentation changes).

## Appendix : Pull request naming

The PR has a descriptive title that a user can understand. We use these titles to generate changelogs for the user.
Most titles use one of these prefixes to categorize the PR e.g. `PREFIX: DESCRIPTION ...`:

* `chore` - Misc changes that aren't dev, feat or fix
* `dev` - Developer related changes
* `enhance` - Enhancements i.e. changes to existing features
* `feat` or `feature` - New features
* `fix` - Bug fixes
* `test` - Test only changes
