#!/bin/bash

runparser() {
  java -jar ../../skeleton/dist/Compiler.jar -target parse $1 2>&1
}

cd `dirname $0`

fail=0

for file in ./illegal/*; do
  out="$(runparser $file)";
  if [ -z "$out" ]; then
    echo "[ ] Illegal file $file incorrectly parsed successfully.";
    fail=1
  else
    echo $out;
    echo "[+]" $file;
    echo
  fi
done

for file in ./legal/*; do
  out="$(runparser $file)";
  if [ -n "$out" ]; then
    echo $out;
    echo "[ ] Legal file $file failed to parse.";
    fail=1
  else
      echo "[+]" $file;
  fi
done

if [ "$fail" -eq 0 ]
then
    echo -e "\nPassed"
else
    echo -e "\nFailed"
fi

exit $fail;
