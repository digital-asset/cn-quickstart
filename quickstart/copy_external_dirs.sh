#!/usr/bin/env bash

LIST_CSV="./links.csv"
LIST_CSV_LINE_COUNT=$(wc -l ${LIST_CSV} | awk '{print 2}')

TMP_DIR='.tmp/'

for i in $(grep -iv '^#' ${LIST_CSV}); do
  repo=$(echo $i | awk -F',' '{print $1}')
  sha=$(echo $i | awk -F',' '{print $2}')
  srcdir=$(echo $i | awk -F',' '{print $3}')
  destdir=$(echo $i | awk -F',' '{print $4}')

  echo "Copying ${srcdir} from ${repo} at ${sha} to ${destdir}"
  mkdir -pv "${TMP_DIR}"
  pushd "${TMP_DIR}"
  git clone ${repo} ${sha}
  pushd "${sha}"
  git checkout "${sha}"
  mkdir -p "../../${destdir}"
  mv "${srcdir}/" "../../${destdir}"
  popd
  popd
  rm -rf "${TMP_DIR}"
  echo ""
done
