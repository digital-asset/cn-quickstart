## Documentation Tooling

This directory contains a [Makefile](Makfile) that provides the following functionality:
- streamline the installation of sphinx and it's dependencies with poetry
- run sphinx to validate and render rst files in to html
- run an http server to preview the rendered html files


## Make Targets

Below covers the provide make targets and what they cover:


### Conversion and Preview
Convert rst to html to validate docs are rendered correctly:

```shell
make render-preview
```

Runs render-preview target and start http server to preview documentation in your browser:

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
