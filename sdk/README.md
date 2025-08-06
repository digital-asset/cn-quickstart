## Documentation Tooling

This directory contains a [Makefile](Makfile) that provides the following functionality:

- streamline the installation of sphinx and it's dependencies via Poetry
- run sphinx to validate and render rst files in to html
- run an http server to preview the rendered html files


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
```

### Rendering and HTML Preview

Convert from RST to HTML to validate documentation is formatted and renders correctly run:

```shell
make render-preview
```

If you want to preview the documentation in your browser, run:

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
