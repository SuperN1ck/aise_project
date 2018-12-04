#!/bin/bash

# Latex clean up script by Nick Heppert
# Contact: nick.heppert@gmx.de

# CLEAN UP
# How to use:
#   Pass folder directory to the script
# e.g. ./clean_up.sh .
#      clean the current directory

shopt -s extglob nocaseglob
cd $1
for file in !(@(*.sty|*.pdf|*.tex|*.sh|*.txss|*.xmpdata|*.bib|*.cls|*.bst|*.sty|*.eps|*.png)); do
    [[ -f "${file}" ]] && files+=( "${file}" )
done
(( ${#files[@]} )) && rm "${files[@]}" 
