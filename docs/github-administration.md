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
4. Keep the `Backend CI` and `Frontend CI` workflows running on every PR for
   visibility.
5. The current relaxed setting does not make either `build-and-test` check a
   required merge check; restore those requirements when the team is ready for
   strict CI gating.
6. Block force pushes and branch deletion.
7. Apply the rule to administrators where organization policy permits it.

The backend workflow is defined in `.github/workflows/backend-ci.yml` and runs
`./mvnw verify`, including the Testcontainers MySQL integration tests. The
frontend workflow is defined in `.github/workflows/frontend-ci.yml` and runs
`npm ci` followed by `npm run build` (TypeScript checking and Vite production
build). Both report pass/fail on every PR, but the relaxed branch rule currently
permits a merge after the required review even if either check fails.

## Pull request policy

- Work on a feature branch.
- Open a pull request into `main`.
- Obtain one review from another team member.
- Merge only after both `Backend CI / build-and-test` and `Frontend CI /
  build-and-test` succeed.
- Simon additionally reviews AI-related pull requests for authenticated-user
  scoping.
