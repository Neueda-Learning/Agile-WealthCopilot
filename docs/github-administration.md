# GitHub Administration

Repository: `Neueda-Learning/Agile-WealthCopilot`

## Required collaborators

The project description requires both instructors to have repository access:

- `helppo2`
- `tuistmessiah`

For an organization repository, grant the lowest role that allows source
review (`Read`). Do not grant write or administration access unless an
instructor explicitly requests it.

## Main branch protection

Protect `main` with these settings:

1. Require a pull request before merging.
2. Require at least one approving review.
3. Dismiss stale approvals when new commits are pushed.
4. Keep the `Backend CI` workflow running on every PR for visibility.
5. The current relaxed setting does not make `build-and-test` a required merge
   check; restore that requirement when the team is ready for strict CI gating.
6. Block force pushes and branch deletion.
7. Apply the rule to administrators where organization policy permits it.

The workflow is defined in `.github/workflows/backend-ci.yml` and runs
`./mvnw verify`, including the Testcontainers MySQL integration tests. It still
reports pass/fail on every PR, but the relaxed branch rule currently permits a
merge after the required review even if that check fails.

## Pull request policy

- Work on a feature branch.
- Open a pull request into `main`.
- Obtain one review from another team member.
- Merge only after `build-and-test` succeeds.
- Simon additionally reviews AI-related pull requests for authenticated-user
  scoping.
