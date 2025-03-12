#!/usr/bin/env bash

# install requirements
brew install pandoc
brew install basictex

# create pdf files
pandoc ../guide/installation.md -o ../guide/installation.pdf --pdf-engine=xelatex
