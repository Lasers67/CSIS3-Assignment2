#!/bin/bash

# Define arrays for var1, var2, and var3 values
var1_values=("standard" "simple" "stop" "english")
var3_values=("standard" "simple" "stop" "english")
var2_values=("classic" "bm25")

# Loop over all combinations of var1, var2, and var3
for var1 in "${var1_values[@]}"; do
  for var2 in "${var2_values[@]}"; do
	  for var3 in "${var3_values[@]}";do
      java -jar target/example1-1.2-createindex.jar $var1 &&
      java -jar target/example1-1.2-lucenesearch.jar $var3 $var2 &&
      cd ../trec_eval-9.0.7/ && \
      ./trec_eval -m map -m P.5 ../cranqrel_modified.txt ../example1-create-index/results.txt && \
      cd ../example1-create-index
      
      # Check if the command execution was successful
      if [ $? -ne 0 ]; then
        echo "Error running with var1=$var1, var2=$var2"
        exit 1
      fi
  done
done
done

