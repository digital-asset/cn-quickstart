# Documentation Tooling

This directory contains the source files for user facing documentation and tooling to validate and test changes.

There is a [Makefile](Makfile) that provides the following functionality:

- streamline the installation of sphinx and it's dependencies via Poetry
- run vale to do prose linting to ensure consistency
- run sphinx to validate and render rst files in to html
- run an http server to preview the rendered html files


## Setup

You will need to run `direnv allow` when you work in this directory. This is because we override the projects default
nix flake shell by loading additional packages that are used in CI. This will ensure that all documentation related 
tooling is provided by nix packages that would not be provided to Quickstart users.

## Make Targets

Provided make targets and their descriptions:

```
$> make help
Usage: make [target]

Available targets:
  clean-all            run all clean targets
  clean-preview-dir    remove the .preview/ directory
  clean-venv-dir       remove python virtual environment directory
  help                 Show this help message
  host-preview         start http server to enable viewing of render-preview output
  poetry-install       use poetry to install python modules
  render-preview       use sphinx to render html version of docs/user/ documentation
  vale-errors          check for errors with vale prose linter of user facing docs
  vale-suggestions     check for suggestions with vale prose linter of user facing docs
```

### Prose linting with vale

Check for errors with vale:

```shell
make vale-errors
```

Check for suggestions and warnings with vale:

```shell
make vale-suggestions
```

### Rendering and HTML Preview

Convert from RST to HTML to validate documentation is formatted and renders correctly run:

```shell
make render-preview
```

If you want to preview the documentation in your browser, run and open the url provided by the target:

```shell
make host-preview
```

### Cleaning Things Up

Remove rendered html files:

```shell
make clean-preview-dir
```

Remove python virtual environment:

```shell
make clean-venv-dir
```
Execute both clean-preview-dir and clean-venv-dir:

```shell
make clean-all
```
